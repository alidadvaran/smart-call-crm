'use strict';
const db = require('./db');

// Android CallLog.Calls.TYPE codes.
const T_INCOMING = 1;
const T_OUTGOING = 2;
const T_MISSED = 3;
const T_REJECTED = 5;

function rangeForType(type) {
  const now = Date.now();
  if (type === 'DAILY') return { since: now - 24 * 3600 * 1000, title: 'گزارش روزانه' };
  if (type === 'WEEKLY') return { since: now - 7 * 24 * 3600 * 1000, title: 'گزارش هفتگی' };
  return { since: now - 24 * 3600 * 1000, title: 'گزارش دستی' }; // MANUAL
}

function fmtDuration(sec) {
  sec = Number(sec) || 0;
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  const p = (n) => String(n).padStart(2, '0');
  return h > 0 ? `${h}:${p(m)}:${p(s)}` : `${p(m)}:${p(s)}`;
}

async function computeAggregates(since) {
  const perDevice = (await db.query(
    `SELECT d.name, d.role,
            COUNT(c.id) AS total,
            COUNT(c.id) FILTER (WHERE c.type = $2) AS incoming,
            COUNT(c.id) FILTER (WHERE c.type = $3) AS outgoing,
            COUNT(c.id) FILTER (WHERE c.type = $4) AS missed,
            COUNT(c.id) FILTER (WHERE c.type = $5) AS rejected,
            COALESCE(SUM(c.duration_sec), 0) AS talk
     FROM devices d
     LEFT JOIN calls c ON c.device_uuid = d.device_uuid AND c.started_at >= $1
     GROUP BY d.id, d.name, d.role
     ORDER BY d.role`,
    [since, T_INCOMING, T_OUTGOING, T_MISSED, T_REJECTED]
  )).rows;

  const totals = (await db.query(
    `SELECT COUNT(*) AS total,
            COUNT(*) FILTER (WHERE type = $2) AS incoming,
            COUNT(*) FILTER (WHERE type = $3) AS outgoing,
            COUNT(*) FILTER (WHERE type = $4) AS missed,
            COUNT(*) FILTER (WHERE type = $5) AS rejected,
            COALESCE(SUM(duration_sec), 0) AS talk
     FROM calls WHERE started_at >= $1`,
    [since, T_INCOMING, T_OUTGOING, T_MISSED, T_REJECTED]
  )).rows[0];

  const topNumbers = (await db.query(
    `SELECT number_normalized AS number, MAX(contact_name) AS name, COUNT(*) AS count
     FROM calls WHERE started_at >= $1
     GROUP BY number_normalized ORDER BY count DESC LIMIT 10`,
    [since]
  )).rows;

  const newCustomers = Number((await db.query(
    `SELECT COUNT(*) AS n FROM customers WHERE created_at >= to_timestamp($1 / 1000.0)`,
    [since]
  )).rows[0].n);

  const pendingFollowups = Number((await db.query(
    `SELECT COUNT(*) AS n FROM customers WHERE status = 'FOLLOWUP'`
  )).rows[0].n);

  return { perDevice, totals, topNumbers, newCustomers, pendingFollowups };
}

function buildHtml(agg, title) {
  const d = agg.perDevice
    .map(
      (r) => `<tr>
        <td>${esc(r.name)} <small>(${r.role === 'PRIMARY' ? 'اصلی' : 'دوم'})</small></td>
        <td>${r.total}</td><td>${r.incoming}</td><td>${r.outgoing}</td>
        <td>${r.missed}</td><td>${r.rejected}</td><td>${fmtDuration(r.talk)}</td>
      </tr>`
    )
    .join('');

  const top = agg.topNumbers
    .map((r) => `<tr><td>${esc(r.name || r.number || 'ناشناس')}</td><td>${esc(r.number)}</td><td>${r.count}</td></tr>`)
    .join('');

  const t = agg.totals;
  return `<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8">
  <style>
    body{font-family:Tahoma,Arial,sans-serif;background:#f6f8f8;color:#222;padding:16px}
    .card{background:#fff;border-radius:10px;padding:16px;margin-bottom:16px;box-shadow:0 1px 4px rgba(0,0,0,.08)}
    h1{color:#0D6E6E;font-size:20px} h2{font-size:16px;color:#0D6E6E}
    table{width:100%;border-collapse:collapse} th,td{border-bottom:1px solid #eee;padding:8px;text-align:right;font-size:13px}
    th{background:#0D6E6E;color:#fff}
    .kpis{display:flex;flex-wrap:wrap;gap:10px}
    .kpi{flex:1;min-width:120px;background:#e6f2f2;border-radius:8px;padding:12px}
    .kpi b{font-size:20px;display:block;color:#0D6E6E}
  </style></head><body>
  <div class="card"><h1>${esc(title)}</h1>
    <div class="kpis">
      <div class="kpi"><b>${t.total}</b>کل تماس‌ها</div>
      <div class="kpi"><b>${t.incoming}</b>ورودی</div>
      <div class="kpi"><b>${t.outgoing}</b>خروجی</div>
      <div class="kpi"><b>${t.missed}</b>ازدست‌رفته</div>
      <div class="kpi"><b>${fmtDuration(t.talk)}</b>مجموع مکالمه</div>
      <div class="kpi"><b>${agg.newCustomers}</b>مشتریان جدید</div>
      <div class="kpi"><b>${agg.pendingFollowups}</b>پیگیری‌های معوق</div>
    </div>
  </div>
  <div class="card"><h2>به تفکیک دستگاه</h2>
    <table><tr><th>دستگاه</th><th>کل</th><th>ورودی</th><th>خروجی</th><th>ازدست‌رفته</th><th>ردشده</th><th>مکالمه</th></tr>
    ${d || '<tr><td colspan="7">داده‌ای نیست</td></tr>'}</table>
  </div>
  <div class="card"><h2>پرتکرارترین شماره‌ها</h2>
    <table><tr><th>نام</th><th>شماره</th><th>تعداد</th></tr>
    ${top || '<tr><td colspan="3">داده‌ای نیست</td></tr>'}</table>
  </div>
  <p style="color:#888;font-size:12px">این گزارش به‌صورت خودکار توسط Smart Call CRM تولید شده است.</p>
  </body></html>`;
}

function buildCsv(agg) {
  const lines = [];
  lines.push('بخش,نام,نقش,کل,ورودی,خروجی,ازدست‌رفته,ردشده,مدت_مکالمه_ثانیه');
  for (const r of agg.perDevice) {
    lines.push(
      `device,${csv(r.name)},${r.role},${r.total},${r.incoming},${r.outgoing},${r.missed},${r.rejected},${r.talk}`
    );
  }
  const t = agg.totals;
  lines.push(`total,همه,-,${t.total},${t.incoming},${t.outgoing},${t.missed},${t.rejected},${t.talk}`);
  lines.push('');
  lines.push('شماره_پرتکرار,نام,تعداد');
  for (const r of agg.topNumbers) lines.push(`${csv(r.number)},${csv(r.name || '')},${r.count}`);
  lines.push('');
  lines.push(`مشتریان_جدید,${agg.newCustomers}`);
  lines.push(`پیگیری_معوق,${agg.pendingFollowups}`);
  return '﻿' + lines.join('\n'); // BOM so Excel reads UTF-8/Persian correctly
}

function esc(s) {
  return String(s == null ? '' : s).replace(/[&<>"]/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])
  );
}
function csv(s) {
  const v = String(s == null ? '' : s);
  return /[",\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
}

module.exports = { rangeForType, computeAggregates, buildHtml, buildCsv, fmtDuration };
