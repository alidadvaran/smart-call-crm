'use strict';
const express = require('express');
const fs = require('fs');
const path = require('path');
const db = require('./db');

const APK_DIR = process.env.APK_DIR || path.join(__dirname, '..', 'apk');
const APK_PATH = path.join(APK_DIR, 'app-latest.apk');

function baseUrl(req) {
  if (process.env.PUBLIC_BASE_URL) return process.env.PUBLIC_BASE_URL.replace(/\/$/, '');
  return `${req.protocol}://${req.get('host')}`;
}

/**
 * Registers the self-update endpoints.
 *  - POST /api/app/upload   (admin token)  — receive a new APK + version
 *  - GET  /api/app/latest   (device token) — latest version metadata
 *  - GET  /api/app/download (device token) — stream the APK
 */
function register(app, requireDevice) {
  fs.mkdirSync(APK_DIR, { recursive: true });

  // Upload a new APK. Protected by a static admin token (CI or you), NOT a
  // device token. Body is the raw APK bytes; version comes from the query.
  app.post(
    '/api/app/upload',
    express.raw({ type: ['application/vnd.android.package-archive', 'application/octet-stream'], limit: '100mb' }),
    async (req, res) => {
      const token = req.get('x-admin-token');
      if (!process.env.APP_UPLOAD_TOKEN || token !== process.env.APP_UPLOAD_TOKEN) {
        return res.status(401).json({ error: 'invalid admin token' });
      }
      const versionCode = parseInt(req.query.versionCode, 10);
      const versionName = String(req.query.versionName || '');
      if (!versionCode || !Buffer.isBuffer(req.body) || req.body.length === 0) {
        return res.status(400).json({ error: 'versionCode and APK body required' });
      }
      try {
        fs.mkdirSync(APK_DIR, { recursive: true });
        fs.writeFileSync(APK_PATH, req.body);
        await db.query(
          `INSERT INTO app_release(id, version_code, version_name, file_path, size_bytes, uploaded_at)
           VALUES (1,$1,$2,$3,$4, now())
           ON CONFLICT (id) DO UPDATE SET
             version_code = EXCLUDED.version_code,
             version_name = EXCLUDED.version_name,
             file_path = EXCLUDED.file_path,
             size_bytes = EXCLUDED.size_bytes,
             uploaded_at = now()`,
          [versionCode, versionName, APK_PATH, req.body.length]
        );
        res.json({ ok: true, versionCode, versionName, sizeBytes: req.body.length });
      } catch (e) {
        console.error('[app upload]', e.message);
        res.status(500).json({ error: 'upload failed' });
      }
    }
  );

  app.get('/api/app/latest', requireDevice, async (req, res) => {
    const r = await db.query('SELECT version_code, version_name, size_bytes FROM app_release WHERE id = 1');
    if (r.rowCount === 0) return res.json({ versionCode: 0, versionName: '', url: null });
    const row = r.rows[0];
    res.json({
      versionCode: row.version_code,
      versionName: row.version_name,
      sizeBytes: Number(row.size_bytes),
      url: `${baseUrl(req)}/api/app/download`,
    });
  });

  app.get('/api/app/download', requireDevice, async (_req, res) => {
    if (!fs.existsSync(APK_PATH)) return res.status(404).json({ error: 'no apk' });
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="app-latest.apk"');
    fs.createReadStream(APK_PATH).pipe(res);
  });
}

module.exports = { register, APK_DIR, APK_PATH };
