package net.gini.android.capture.network.logging

import net.gini.android.bank.api.BuildConfig
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.core.api.Resource
import java.io.PrintWriter
import java.io.StringWriter

internal fun ErrorLog.toErrorEvent(): ErrorEvent =
    ErrorEvent(
        deviceModel,
        osName,
        osVersion,
        captureVersion,
        BuildConfig.VERSION_NAME,
        description = exception?.let {
            "$description; Exception: ${it.stackTraceString}"
        } ?: description
    )

internal val Throwable.stackTraceString: String
    get() {
        return StringWriter().let { sw ->
            printStackTrace(PrintWriter(sw))
            sw.toString()
        }
    }

internal val Resource.Error<*>.formattedErrorMessage: String
    get() = """
        Message: ${message ?: "unknown"}
        Response status code: ${responseStatusCode ?: "n/a"}
        Response headers: ${responseHeaders ?: "n/a"}
        Response body: ${responseBody ?: "n/a"}
        Exception: ${exception ?: "n/a"}
    """.trimIndent()
