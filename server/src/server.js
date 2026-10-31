'use strict';
require('dotenv').config();
const express = require('express');
const basicAuth = require('basic-auth');
const db = require('./db');
const { generateToken, hashToken, requireDevice } = require('./auth');
const { sendReport } = require('./mailer');
const { computeAggregates, buildHtml } = require('./reports');
const { startCron } = require('./cron');
const appupdate = require('./appupdate');

const app = express();
app.use(express.json({ limit: '8mb' }));

app.get('/healthz', (_req, res) => res.json({ ok: true }));

// ---- Device registration ------------------------------------------------
app.post('/api/devices/register', async (req, res) => {
  const { deviceUuid, name, role } = req.body || {};
  if (!deviceUuid || !name) return res.status(400).json({ error: 'deviceUuid and name required' });
  const token = generateToken();
  try {
    const result = await db.query(
      `INSERT INTO devices(device_uuid, name, role, token_hash)
       VALUES ($1,$2,$3,$4)
       ON CONFLICT (device_uuid)
       DO UPDATE SET name = EXCLUDED.name, role = EXCLUDED.role, token_hash = EXCLUDED.token_hash
       RETURNING id`,
      [deviceUuid, name, role || 'PRIMARY', hashToken(token)]
    );
    res.json({ deviceId: result.rows[0].id, token });
  } catch (e) {
    console.error('[register]', e.message);
    res.status(500).json({ error: 'registration failed' });
  }
});

// ---- Call upload (idempotent) -------------------------------------------
app.post('/api/calls/upload', requireDevice, async (req, res) => {
  const calls = Array.isArray(req.body?.calls) ? req.body.calls : [];
  let accepted = 0;
  let duplicates = 0;
  try {
    await db.tx(async (client) => {
      for (const c of calls) {
        const ins = await client.query(
          `INSERT INTO calls(client_uuid, device_id, device_uuid, source_id, raw_number,
                             number_normalized, contact_name, type, started_at, duration_sec,
                             phone_account_id, category)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
           ON CONFLICT (device_uuid, client_uuid) DO NOTHING
           RETURNING id`,
          [
            c.clientUuid, req.device.id, req.device.device_uuid, c.sourceId || 0,
            c.number || '', c.numberNormalized || '', c.contactName || null,
            c.type || 0, c.startedAt || 0, c.durationSec || 0,
            c.phoneAccountId || null, c.category || 'UNSET',
          ]
        );
        if (ins.rowCount === 0) { duplicates++; continue; }
        accepted++;
        const num = c.numberNormalized;
        if (num) {
          await client.query(
            `INSERT INTO customers(number_normalized, display_name, last_call_at, call_count)
             VALUES ($1,$2,$3,0) ON CONFLICT (number_normalized) DO NOTHING`,
            [num, c.contactName || num, c.startedAt || 0]
          );
          const up = await client.query(
            `UPDATE customers
             SET call_count = call_count + 1,
                 last_call_at = GREATEST(last_call_at, $2),
                 updated_at = now()
             WHERE number_normalized = $1 RETURNING id`,
            [num, c.startedAt || 0]
          );
          if (up.rowCount > 0) {
            await client.query('UPDATE calls SET customer_id = $1 WHERE id = $2', [
              up.rows[0].id, ins.rows[0].id,
            ]);
          }
        }
      }
    });
    res.json({ accepted, duplicates });
  } catch (e) {
    console.error('[upload calls]', e.message);
    res.status(500).json({ error: 'upload failed' });
  }
});

// ---- Customer profile upload --------------------------------------------
app.post('/api/customers/upload', requireDevice, async (req, res) => {
  const customers = Array.isArray(req.body?.customers) ? req.body.customers : [];
  try {
    await db.tx(async (client) => {
      for (const c of customers) {
        if (!c.numberNormalized) continue;
        await client.query(
          `INSERT INTO customers(number_normalized, display_name, company, status, notes)
           VALUES ($1,$2,$3,$4,$5)
           ON CONFLICT (number_normalized) DO UPDATE SET
             display_name = EXCLUDED.display_name,
             company = COALESCE(EXCLUDED.company, customers.company),
             status  = EXCLUDED.status,
             notes   = COALESCE(EXCLUDED.notes, customers.notes),
             updated_at = now()`,
          [c.numberNormalized, c.displayName || c.numberNormalized, c.company || null,
           c.status || 'NEW', c.notes || null]
        );
      }
    });
    res.json({ ok: true });
  } catch (e) {
    console.error('[upload customers]', e.message);
    res.status(500).json({ error: 'upload failed' });
  }
});

// ---- Trigger an email report --------------------------------------------
app.post('/api/reports/send', requireDevice, async (req, res) => {
  const type = (req.body?.type || 'MANUAL').toUpperCase();
  const recipient = req.body?.recipient;
  const result = await sendReport(type, recipient);
  res.json({ ok: result.ok, message: result.message });
});

// ---- Self-update endpoints (upload / latest / download) -----------------
appupdate.register(app, requireDevice);

// ---- Minimal web dashboard (HTTP Basic auth) ----------------------------
function dashboardAuth(req, res, next) {
  const user = process.env.DASHBOARD_USER;
  const pass = process.env.DASHBOARD_PASS;
  if (!user || !pass) return res.status(503).send('Dashboard credentials not configured.');
  const cred = basicAuth(req);
  if (!cred || cred.name !== user || cred.pass !== pass) {
    res.set('WWW-Authenticate', 'Basic realm="Smart Call CRM"');
    return res.status(401).send('نیاز به ورود.');
  }
  next();
}

app.get('/', dashboardAuth, async (_req, res) => {
  try {
    const agg = await computeAggregates(0); // all-time
    const recent = (await db.query(
      `SELECT c.raw_number, c.contact_name, c.type, c.started_at, c.duration_sec, d.name AS device
       FROM calls c JOIN devices d ON d.id = c.device_id
       ORDER BY c.started_at DESC LIMIT 50`
    )).rows;
    const rows = recent
      .map((r) => `<tr><td>${esc(r.contact_name || r.raw_number || '—')}</td>
        <td>${esc(r.device)}</td><td>${typeLabel(r.type)}</td>
        <td>${new Date(Number(r.started_at)).toLocaleString('fa-IR')}</td>
        <td>${r.duration_sec}s</td></tr>`)
      .join('');
    const html = buildHtml(agg, 'داشبورد مرکزی — همه زمان‌ها').replace(
      '</body>',
      `<div class="card"><h2>آخرین ۵۰ تماس (هر دو گوشی)</h2>
       <table><tr><th>مخاطب</th><th>دستگاه</th><th>نوع</th><th>زمان</th><th>مدت</th></tr>
       ${rows || '<tr><td colspan="5">داده‌ای نیست</td></tr>'}</table></div></body>`
    );
    res.send(html);
  } catch (e) {
    console.error('[dashboard]', e.message);
    res.status(500).send('خطا در ساخت داشبورد');
  }
});

function typeLabel(t) {
  return { 1: 'ورودی', 2: 'خروجی', 3: 'ازدست‌رفته', 5: 'ردشده' }[t] || 'نامشخص';
}
function esc(s) {
  return String(s == null ? '' : s).replace(/[&<>"]/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`[server] Smart Call CRM listening on :${port}`);
  startCron();
});
