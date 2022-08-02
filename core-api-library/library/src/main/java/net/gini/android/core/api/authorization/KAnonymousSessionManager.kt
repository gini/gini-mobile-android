package net.gini.android.core.api.authorization

import kotlinx.coroutines.flow.Flow
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel

class KAnonymousSessionManager(
    userRepository: UserRepository,
    credentialsStore: CredentialsStore,
    emailDomain: String,
    currentSession: Session
): KSessionManager {
    val userRepository: UserRepository

    init {
        this.userRepository = userRepository
    }

    override suspend fun getSession(): Session {
        
    }

    suspend fun loginUser(userRequestModel: UserRequestModel): Flow<UserResponseModel?> {
        return userRepository.loginUser(userRequestModel)
    }
}
