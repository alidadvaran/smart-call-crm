package ir.smartcallcrm.core

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Date/time helpers, including a Gregorian→Jalali (Persian/Shamsi) converter
 * so the Persian UI shows familiar dates. All formatting is pure-Kotlin and
 * dependency-free.
 */
object TimeUtils {

    private val persianMonths = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    data class Jalali(val year: Int, val month: Int, val day: Int)

    /** Convert a Gregorian date to Jalali. */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Jalali {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) gDayNo += gDaysInMonth[i]
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) gDayNo++
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var i = 0
        while (i < 11 && jDayNo >= jDaysInMonth[i]) {
            jDayNo -= jDaysInMonth[i]; i++
        }
        return Jalali(jy, i + 1, jDayNo + 1)
    }

    /** "۱۴۰۳/۰۵/۲۱  ۱۴:۳۰" style label for a call timestamp. */
    fun formatJalaliDateTime(epochMillis: Long, persianDigits: Boolean = true): String {
        if (epochMillis <= 0) return "—"
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = epochMillis
        val j = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val out = String.format(
            Locale.US, "%04d/%02d/%02d  %02d:%02d", j.year, j.month, j.day, h, min
        )
        return if (persianDigits) toPersianDigits(out) else out
    }

    fun formatJalaliDate(epochMillis: Long, persianDigits: Boolean = true): String {
        if (epochMillis <= 0) return "—"
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        val j = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val out = "${j.day} ${persianMonths[j.month - 1]} ${j.year}"
        return if (persianDigits) toPersianDigits(out) else out
    }

    /** Human duration: mm:ss, or h:mm:ss for calls over an hour. */
    fun formatDuration(seconds: Long, persianDigits: Boolean = true): String {
        val s = if (seconds < 0) 0 else seconds
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val out = if (h > 0)
            String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else
            String.format(Locale.US, "%02d:%02d", m, sec)
        return if (persianDigits) toPersianDigits(out) else out
    }

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(if (ch in '0'..'9') '۰' + (ch - '0') else ch)
        }
        return sb.toString()
    }

    /** Start-of-day epoch millis, [daysBack] days before today (local time). */
    fun startOfDayMillis(daysBack: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
