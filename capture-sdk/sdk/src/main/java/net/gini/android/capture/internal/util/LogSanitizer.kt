package net.gini.android.capture.internal.util

/**
 * Internal use only.
 *
 * Utility to sanitize log inputs against CWE-117 (Log Injection).
 * Strips newline and tab characters from external/user-controlled data
 * before it is written to log statements.
 */
object LogSanitizer {
    @JvmStatic
    fun sanitize(input: Any?): String =
        input?.toString()
            ?.replace("\n", "\\n")
            ?.replace("\r", "\\r")
            ?.replace("\t", "\\t")
            ?: "null"
}

