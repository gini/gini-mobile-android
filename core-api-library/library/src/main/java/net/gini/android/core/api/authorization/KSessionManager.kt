package net.gini.android.core.api.authorization

interface KSessionManager {
    suspend fun getSession(): Session
}
