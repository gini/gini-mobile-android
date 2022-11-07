package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.Utils
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import org.json.JSONObject
import java.util.*

class AnonymousSessionManager(
    private val userRepository: UserRepository,
    private val credentialsStore: CredentialsStore,
    private val emailDomain: String
): SessionManager {
    private var currentSession: Session? = null

    override suspend fun getSession(): Resource<Session> {
        currentSession?.let { session ->
            if (!session.hasExpired()) {
                return Resource.Success(session)
            }
        }

        val userCredentials = credentialsStore.userCredentials
        return if (userCredentials == null) {
            when (val createResponse = createUser()) {
                is Resource.Cancelled -> Resource.Cancelled()
                is Resource.Error -> Resource.Error(createResponse)
                is Resource.Success -> {
                    val loginResponse = loginUser()
                    currentSession = when (loginResponse) {
                        is Resource.Success -> loginResponse.data
                        else -> null
                    }
                    loginResponse
                }
            }
        } else {
            return when (val loginResponse = loginUser()) {
                is Resource.Success -> {
                    currentSession = loginResponse.data
                    loginResponse
                }
                is Resource.Error -> {
                    if (isInvalidUserError(loginResponse)) {
                        currentSession = null
                        credentialsStore.deleteUserCredentials()
                        return when (val createResponse = createUser()) {
                            is Resource.Cancelled -> Resource.Cancelled()
                            is Resource.Error -> Resource.Error(createResponse)
                            is Resource.Success -> {
                                val newUserLoginResponse = loginUser()
                                currentSession = when (newUserLoginResponse) {
                                    is Resource.Success -> newUserLoginResponse.data
                                    else -> null
                                }
                                newUserLoginResponse
                            }
                        }
                    } else {
                        loginResponse
                    }
                }
                is Resource.Cancelled -> loginResponse
            }
        }
    }

    private suspend fun createUser(): Resource<Unit> {
        val userRequestModel = UserRequestModel(generateUserName(), generatePassword())
        return when (val response = userRepository.createUser(userRequestModel)) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(response)
            is Resource.Success -> {
                credentialsStore.storeUserCredentials(UserCredentials(userRequestModel.email, userRequestModel.password))
                response
            }
        }
    }

    suspend fun loginUser(): Resource<Session> {
        val userCredentials = credentialsStore.userCredentials
        if (userCredentials != null) {
            if (hasUserCredentialsEmailDomain(emailDomain, userCredentials)) {
                return userRepository.loginUser(UserRequestModel(userCredentials.username, userCredentials.password))
            }

            return when (val updateCredentials = updateEmailDomain(userCredentials)) {
                is Resource.Cancelled -> Resource.Cancelled()
                is Resource.Error -> Resource.Error(updateCredentials)
                is Resource.Success -> {
                    userRepository.loginUser(UserRequestModel(updateCredentials.data.username, updateCredentials.data.password))
                }
            }
        }
        return Resource.Error()
    }

    private suspend fun updateEmailDomain(userCredentials: UserCredentials): Resource<UserCredentials> {
        val oldEmail = userCredentials.username
        val newEmail = generateUserName()

        return when (val loginUser = userRepository.loginUser(UserRequestModel(userCredentials.username, userCredentials.password))) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(loginUser)
            is Resource.Success -> {
                val session = loginUser.data
                return when (val updateEmail = userRepository.updateEmail(newEmail, oldEmail, session.accessToken)) {
                    is Resource.Cancelled -> Resource.Cancelled()
                    is Resource.Error -> Resource.Error(updateEmail)
                    is Resource.Success -> {
                        credentialsStore.deleteUserCredentials()
                        val newCredentials = UserCredentials(newEmail, userCredentials.password)
                        credentialsStore.storeUserCredentials(newCredentials)
                        Resource.Success(newCredentials)
                    }
                }
            }
        }
    }

    fun hasUserCredentialsEmailDomain(emailDomain: String, userCredentials: UserCredentials): Boolean {
        return userCredentials.username.endsWith("@$emailDomain")
    }

    private fun generateUserName(): String {
        return UUID.randomUUID().toString() + "@" + emailDomain
    }

    private fun generatePassword(): String {
        return UUID.randomUUID().toString()
    }

    private fun isInvalidUserError(resource: Resource<Session>): Boolean {
        when (resource.responseStatusCode ?: 0) {
            400 -> {
                resource.responseBody?.let {
                    val responseJson = JSONObject(String(it.toByteArray(), Utils.CHARSET_UTF8))
                    return responseJson[ERROR_KEY] == GRANT_VALUE
                }
            }
            401 -> return true
        }

        return false
    }

    companion object {
        const val ERROR_KEY = "error"
        const val GRANT_VALUE = "invalid_grant"
    }
}
