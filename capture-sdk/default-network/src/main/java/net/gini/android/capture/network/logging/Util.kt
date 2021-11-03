package net.gini.android.capture.network.logging

import com.android.volley.VolleyError
import net.gini.android.bank.api.BuildConfig
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.core.api.requests.ErrorEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.Charset


internal fun <T : Exception> errorLogFromException(description: String, exception: T): ErrorLog =
    if (exception is VolleyError) {
        ErrorLog(
            description = description,
            exception = RuntimeException(exception.responseDetails, exception)
        )
    } else {
        ErrorLog(description = description, exception = exception)
    }

internal val VolleyError.responseDetails: String
    get() = this.networkResponse?.let { response ->
        val statusCode = response.statusCode
        val headers = response.allHeaders?.toList()?.joinToString("\n") { "${it.name}: ${it.value}" } ?: ""
        val body = response.data?.let { String(it, Charset.forName("UTF-8")) } ?: ""
        return "Status code: $statusCode\nHeaders:\n$headers\nBody:\n$body"
    } ?: this.message ?: toString()

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