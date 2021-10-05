package net.gini.pay.ginipaybusiness.ginipayapi

import android.content.Context
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.core.api.authorization.SessionManager

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
fun getGiniApi(context: Context, sessionManager: SessionManager): GiniHealthAPI {
    return GiniHealthAPIBuilder(context, sessionManager).build()
}