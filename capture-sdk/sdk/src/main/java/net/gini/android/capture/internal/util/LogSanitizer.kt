package net.gini.android.capture.internal.util

/**
 * Internal use only.
 *
 * Utility to sanitize log inputs against CWE-117 (Log Injection).
 * Strips newline and tab characters from external/user-controlled data
 * before it is written to log statements.
 *
 * Returns a lazy wrapper so that sanitization is only performed when the
 * logger actually formats the message (i.e. when the log level is enabled),
 * preserving SLF4J's parameterized-logging lazy evaluation.
 */
object LogSanitizer {
    @JvmStatic
    fun sanitize(input: Any?): Any = object : Any() {
        override fun toString(): String =
            input?.toString()
                ?.replace("\n", "\\n")
                ?.replace("\r", "\\r")
                ?.replace("\t", "\\t")
                ?: "null"
    }
}
