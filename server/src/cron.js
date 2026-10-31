'use strict';
const cron = require('node-cron');
const { sendReport } = require('./mailer');

// Scheduled email reports. Times are server-local; set TZ in the environment
// (e.g. TZ=Asia/Tehran) so these fire at the expected local hour.
function startCron() {
  const recipient = process.env.REPORT_RECIPIENT;

  if (process.env.ENABLE_DAILY_REPORT === 'true') {
    // Every day at 21:00.
    cron.schedule('0 21 * * *', async () => {
      console.log('[cron] daily report');
      await sendReport('DAILY', recipient);
    });
  }

  if (process.env.ENABLE_WEEKLY_REPORT === 'true') {
    // Every Friday at 20:00 (end of the Iranian work week).
    cron.schedule('0 20 * * 5', async () => {
      console.log('[cron] weekly report');
      await sendReport('WEEKLY', recipient);
    });
  }
}

module.exports = { startCron };
