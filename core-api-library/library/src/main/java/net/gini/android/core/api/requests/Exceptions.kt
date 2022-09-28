package net.gini.android.core.api.requests

import java.io.IOException

class ApiException(message: String? = null, responseStatusCode: Int? = null, responseBody: String? = null, headers: Map<String, List<String>>? = null) : IOException(message)
