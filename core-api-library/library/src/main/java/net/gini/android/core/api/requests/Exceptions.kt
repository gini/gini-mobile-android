package net.gini.android.core.api.requests

import retrofit2.Response
import java.io.IOException

class ApiException(
    message: String? = null,
    val responseStatusCode: Int? = null,
    val responseBody: String? = null,
    val responseHeaders: Map<String, List<String>>? = null,
    cause: Throwable? = null
) : IOException(message, cause) {

    constructor(message: String? = null, response: Response<*>) : this(
        message,
        response.code(),
        if (response.isSuccessful) response.body()?.toString() else response.errorBody()?.string(),
        response.headers().toMultimap()
    )
}
