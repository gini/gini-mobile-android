package net.gini.android.core.api.authorization

import net.gini.android.core.api.authorization.apimodels.SessionToken

interface KSessionManager {
    suspend fun getSession(): SessionToken?
}
