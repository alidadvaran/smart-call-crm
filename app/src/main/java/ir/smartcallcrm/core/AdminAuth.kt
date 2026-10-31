package ir.smartcallcrm.core

import java.security.MessageDigest

/**
 * Password gate for sensitive actions (opening Settings, deleting data,
 * turning off sync). The password is never stored in clear text — only its
 * SHA-256 hash is kept (in [SecureStore]). The default password can be
 * changed from inside Settings.
 *
 * Note: this protects against someone using the phone casually. It is not a
 * cryptographic secret against an attacker who has the APK, and it cannot stop
 * an OS-level uninstall — see docs/SECURITY_LOCK.md.
 */
object AdminAuth {
    private const val DEFAULT_PASSWORD = "Ali@101010"

    fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun currentHash(secure: SecureStore): String =
        secure.adminPasswordHash ?: sha256(DEFAULT_PASSWORD)

    fun verify(secure: SecureStore, input: String): Boolean =
        sha256(input) == currentHash(secure)

    fun changePassword(secure: SecureStore, newPassword: String) {
        secure.adminPasswordHash = sha256(newPassword)
    }
}
