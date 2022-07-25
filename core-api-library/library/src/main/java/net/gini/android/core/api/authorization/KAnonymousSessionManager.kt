package net.gini.android.core.api.authorization

import kotlinx.coroutines.runBlocking

class KAnonymousSessionManager(userRepository: UserRepository): KSessionManager {
    val userRepository: UserRepository

    init {
        this.userRepository = userRepository
    }
    override suspend fun getSession(): Session {
        TODO("Not yet implemented")
    }

    suspend fun loginUser(userRequestModel: UserRequestModel): User? {
        return userRepository.loginUser(userRequestModel)
    }
}
