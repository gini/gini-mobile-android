package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.Utils
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import org.json.JSONObject
import java.util.*

/**
 * The [AnonymousSessionManager] is a [SessionManager] implementation that uses anonymous Gini users.
 */
internal class AnonymousSessionManager(
    private val userRepository: UserRepository,
    private val credentialsStore: CredentialsStore,
    private val emailDomain: String
): SessionManager {
    private var currentSession: Session? = null

    override suspend fun getSession(): Resource<Session> {
        currentSession?.takeUnless { it.hasExpired() }?.let { return Resource.Success(it) }
        return if (credentialsStore.userCredentials == null) {
            createUserAndLogin()
        } else {
            loginOrRecreateUser()
        }
    }

    private suspend fun createUserAndLogin(): Resource<Session> {
        return when (val createResponse = createUser()) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(createResponse)
            is Resource.Success -> loginAndCacheSession()
        }
    }

    private suspend fun loginOrRecreateUser(): Resource<Session> {
        return when (val loginResponse = loginUser()) {
            is Resource.Success -> {
                currentSession = loginResponse.data
                loginResponse
            }
            is Resource.Error -> handleLoginError(loginResponse)
            is Resource.Cancelled -> loginResponse
        }
    }

    private suspend fun handleLoginError(error: Resource.Error<Session>): Resource<Session> {
        if (!isInvalidUserError(error)) return error
        currentSession = null
        credentialsStore.deleteUserCredentials()
        return createUserAndLogin()
    }

    private suspend fun loginAndCacheSession(): Resource<Session> {
        val loginResponse = loginUser()
        currentSession = (loginResponse as? Resource.Success)?.data
        return loginResponse
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
            return userRepository.loginUser(UserRequestModel(userCredentials.username, userCredentials.password))
        }
        return Resource.Error()
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
