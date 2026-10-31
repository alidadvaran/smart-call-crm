'use strict';
const crypto = require('crypto');
const db = require('./db');

function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

// Express middleware: requires a valid "Authorization: Bearer <token>" that
// matches a registered device. Attaches req.device.
async function requireDevice(req, res, next) {
  try {
    const header = req.get('authorization') || '';
    const match = header.match(/^Bearer\s+(.+)$/i);
    if (!match) return res.status(401).json({ error: 'missing token' });
    const tokenHash = hashToken(match[1].trim());
    const result = await db.query(
      'SELECT id, device_uuid, name, role FROM devices WHERE token_hash = $1',
      [tokenHash]
    );
    if (result.rowCount === 0) return res.status(401).json({ error: 'invalid token' });
    req.device = result.rows[0];
    await db.query('UPDATE devices SET last_seen = now() WHERE id = $1', [req.device.id]);
    next();
  } catch (e) {
    console.error('[auth] error', e.message);
    res.status(500).json({ error: 'auth failure' });
  }
}

module.exports = { generateToken, hashToken, requireDevice };
