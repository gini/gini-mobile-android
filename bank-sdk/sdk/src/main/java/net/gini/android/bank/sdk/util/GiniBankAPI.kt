package net.gini.android.bank.sdk.util

import android.content.Context
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.core.api.authorization.SessionManager

/**
 * Minimal configuration for Gini Bank API
 */
fun getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String): GiniBankAPI {
    return GiniBankAPIBuilder(context, clientId, clientSecret, emailDomain).build()
}

/**
 * Minimal configuration for Gini Bank API
 */
fun getGiniApi(context: Context, sessionManager: SessionManager): GiniBankAPI {
    return GiniBankAPIBuilder(context, sessionManager = sessionManager).build()
}