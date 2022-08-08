package net.gini.android.core.api.requests

import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.util.concurrent.CancellationException

private const val ERROR_MESSAGE_KEY = "message"

object SafeApiRequest {
    @Throws(ApiException::class, CancellationException::class)
    suspend fun <T : Any?> apiRequest(call: suspend () -> Response<T>): T {
        try {
            val response = call.invoke()
            if (response.isSuccessful) {
                return response.body()!!
            }

            val error = response.errorBody()?.string()
            val message = StringBuilder()
            error?.let {
                try {
                    message.append(JSONObject(it).getString(ERROR_MESSAGE_KEY))
                } catch (e: JSONException) {
                }
            }

            throw ApiException(message.toString(), response.code(), response.body().toString(), response.headers().toMultimap())
        } catch (e: Exception) {
            throw e
        }
    }
}
