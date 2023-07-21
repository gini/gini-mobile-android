package net.gini.android.core.api.requests

/**
 * Created by Alpár Szotyori on 21.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

sealed class HttpHeader(val name: String, val value: String) {

    fun addToMap(mutableMap: MutableMap<String, String>) {
        mutableMap[name] = value
    }

    fun toPair(): Pair<String, String> = name to value
}

class BearerAuthorizatonHeader(token: String): HttpHeader("Authorization", "Bearer $token")
