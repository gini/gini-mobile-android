package net.gini.android.core.api.requests

import java.io.IOException

class ApiException(message: String, responseStatusCode: Int, responseBody: String, headers: Map<String, List<String>>) : IOException(message)
