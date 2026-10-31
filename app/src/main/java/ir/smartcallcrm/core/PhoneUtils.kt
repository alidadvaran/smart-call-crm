package ir.smartcallcrm.core

/**
 * Phone-number helpers. The normalized form is used as the stable key that
 * links the same number across both phones into one shared customer profile.
 */
object PhoneUtils {

    /** Convert Persian (۰-۹) and Arabic (٠-٩) digits to ASCII. */
    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val c = when (ch) {
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Normalize a raw dialed/received number to a comparable key.
     * Iranian numbers are folded to +98XXXXXXXXXX; others keep their digits
     * (with a leading + preserved). Non-numeric junk is stripped.
     */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = toLatinDigits(raw).trim()
        val hasPlus = s.startsWith("+") || s.startsWith("00")
        s = s.filter { it.isDigit() }
        if (s.isEmpty()) return ""

        // International prefixes.
        if (s.startsWith("0098")) s = s.removePrefix("0098")
        else if (hasPlus && s.startsWith("98")) s = s.removePrefix("98")
        else if (s.startsWith("0")) s = s.removePrefix("0") // national trunk 0

        return when {
            // Iranian mobile/landline after trunk removal → +98
            s.length in 9..11 && !raw.startsWith("+") -> "+98$s".take(13)
            raw.startsWith("+") -> "+$s"
            else -> "+98$s".take(13)
        }
    }

    /** A short display label; falls back to the raw number. */
    fun display(raw: String?): String {
        if (raw.isNullOrBlank()) return "ناشناس"
        return toLatinDigits(raw)
    }
}
