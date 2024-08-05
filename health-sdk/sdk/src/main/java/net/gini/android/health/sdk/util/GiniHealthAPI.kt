package net.gini.android.health.sdk.util

import android.content.Context
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String, apiVersion: Int = GiniHealthAPIBuilder.API_VERSION): GiniHealthAPI {
    return GiniHealthAPIBuilder(
        context,
        clientId,
        clientSecret,
        emailDomain,
        apiVersion = apiVersion
    ).build()
}

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, sessionManager: SessionManager, apiVersion: Int = GiniHealthAPIBuilder.API_VERSION): GiniHealthAPI {
    return GiniHealthAPIBuilder(context, sessionManager = sessionManager, apiVersion = apiVersion).build()
}