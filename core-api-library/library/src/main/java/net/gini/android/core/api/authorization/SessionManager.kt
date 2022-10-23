package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.SessionToken

interface SessionManager {
    suspend fun getSession(): Resource<SessionToken>
}
