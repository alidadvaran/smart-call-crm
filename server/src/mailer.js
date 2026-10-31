'use strict';
const nodemailer = require('nodemailer');
const db = require('./db');
const { rangeForType, computeAggregates, buildHtml, buildCsv } = require('./reports');

// Gmail SMTP using an App Password. Credentials live ONLY in the server
// environment (GMAIL_USER / GMAIL_APP_PASSWORD) — never in the Android app.
function transport() {
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.GMAIL_USER,
      pass: process.env.GMAIL_APP_PASSWORD,
    },
  });
}

async function sendReport(type, recipient) {
  const to = recipient || process.env.REPORT_RECIPIENT || process.env.GMAIL_USER;
  const { since, title } = rangeForType(type);
  const agg = await computeAggregates(since);
  const html = buildHtml(agg, title);
  const csv = buildCsv(agg);
  const stamp = new Date().toISOString().slice(0, 10);

  try {
    if (!process.env.GMAIL_USER || !process.env.GMAIL_APP_PASSWORD) {
      throw new Error('GMAIL_USER / GMAIL_APP_PASSWORD not configured');
    }
    await transport().sendMail({
      from: `Smart Call CRM <${process.env.GMAIL_USER}>`,
      to,
      subject: `${title} — ${stamp}`,
      html,
      attachments: [
        { filename: `report-${type.toLowerCase()}-${stamp}.csv`, content: csv, contentType: 'text/csv; charset=utf-8' },
      ],
    });
    await db.query(
      'INSERT INTO report_log(type, recipient, ok, detail) VALUES ($1,$2,true,$3)',
      [type, to, 'sent']
    );
    return { ok: true };
  } catch (e) {
    await db.query(
      'INSERT INTO report_log(type, recipient, ok, detail) VALUES ($1,$2,false,$3)',
      [type, to, e.message]
    );
    return { ok: false, message: e.message };
  }
}

module.exports = { sendReport };
