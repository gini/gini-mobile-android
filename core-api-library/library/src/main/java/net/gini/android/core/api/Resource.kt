package net.gini.android.core.api

open class Resource<T>(
    val data: T? = null,
    val responseStatusCode: Int? = null,
    val responseHeaders: Map<String, List<String>>? = null,
    val responseBody: String? = null
) {
    class Success<T>(data: T,
                     responseStatusCode: Int? = null,
                     responseHeaders: Map<String, List<String>>? = null,
                     responseBody: String? = null)
        : Resource<T>(data, responseStatusCode, responseHeaders, responseBody)

    class Error<T>(var message: String,
                   var errorCode: Int? = null,
                   data: T? = null,
                   responseStatusCode: Int? = null,
                   responseHeaders: Map<String, List<String>>? = null,
                   responseBody: String? = null)
        : Resource<T>(data, responseStatusCode, responseHeaders, responseBody)

    class Cancelled<T>(): Resource<T>()
}
