package net.gini.pay.ginipaybusiness.ginipayapi

import android.content.Context
import net.gini.android.health.api.Gini
import net.gini.android.health.api.GiniBuilder
import net.gini.android.core.api.authorization.SessionManager

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String): Gini {
    return GiniBuilder(context, clientId, clientSecret, emailDomain).build()
}

/**
 * Minimal configuration for Gini API
 */
fun getGiniApi(context: Context, sessionManager: SessionManager): Gini {
    return GiniBuilder(context, sessionManager).build()
}