package net.gini.android.health.sdk.util

import android.content.Context
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String): GiniHealthAPI {
    return GiniHealthAPIBuilder(
        context,
        clientId,
        clientSecret,
        emailDomain
    ).build()
}

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, sessionManager: KSessionManager): GiniHealthAPI {
    return GiniHealthAPIBuilder(context, sessionManager = sessionManager).build()
}