'use strict';
// Minimal forward-only migration runner: applies every .sql file in
// ../migrations in name order, tracking applied files in a table.
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { pool } = require('./db');

async function run() {
  await pool.query(
    'CREATE TABLE IF NOT EXISTS _migrations (name TEXT PRIMARY KEY, applied_at TIMESTAMPTZ DEFAULT now())'
  );
  const dir = path.join(__dirname, '..', 'migrations');
  const files = fs.readdirSync(dir).filter((f) => f.endsWith('.sql')).sort();
  for (const file of files) {
    const done = await pool.query('SELECT 1 FROM _migrations WHERE name = $1', [file]);
    if (done.rowCount > 0) {
      console.log(`[migrate] skip ${file}`);
      continue;
    }
    const sql = fs.readFileSync(path.join(dir, file), 'utf8');
    console.log(`[migrate] apply ${file}`);
    await pool.query(sql);
    await pool.query('INSERT INTO _migrations(name) VALUES ($1)', [file]);
  }
  console.log('[migrate] done');
  await pool.end();
}

run().catch((e) => {
  console.error('[migrate] failed', e);
  process.exit(1);
});
