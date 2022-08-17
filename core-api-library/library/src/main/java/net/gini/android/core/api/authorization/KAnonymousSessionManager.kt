package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.Utils
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import okhttp3.ResponseBody
import org.json.JSONObject
import java.util.*

class KAnonymousSessionManager(
    private val userRepository: UserRepository,
    private val credentialsStore: CredentialsStore,
    private val emailDomain: String
): KSessionManager {
    private var currentSession: SessionToken? = null

    override suspend fun getSession(): SessionToken? {
        if (currentSession != null && !currentSession!!.hasExpired()) {
            return currentSession
        }

        val userCredentials = credentialsStore.userCredentials
        if (userCredentials == null) {
            var createResponse = createUser()
            if (createResponse is Resource.Success) {
                when (val loginResponse = loginUser()) {
                    is Resource.Success -> {
                        currentSession = loginResponse.data
                        return currentSession
                    }

                    is Resource.Error -> {
                        if (isInvalidUserError(loginResponse)) {
                            currentSession = null
                            credentialsStore.deleteUserCredentials()
                            createResponse = createUser()
                            if (createResponse is Resource.Success) {
                                currentSession = loginUser().data
                            }
                        }
                    }

                    is Resource.Cancelled -> {
                        currentSession = null
                    }
                }
            }
        }

        return null
    }

    suspend fun createUser(): Resource<ResponseBody> {
        val userRequestModel = UserRequestModel(generateUserName(), generatePassword())
        val response = userRepository.createUser(userRequestModel)
        if (response is Resource.Success) {
            credentialsStore.storeUserCredentials(UserCredentials(userRequestModel.username, userRequestModel.password))
        }
        return response
    }

    suspend fun loginUser(): Resource<SessionToken> {
        val userCredentials = credentialsStore.userCredentials
        if (userCredentials != null) {
            if (hasUserCredentialsEmailDomain(emailDomain, userCredentials)) {
                return userRepository.loginUser(UserRequestModel(userCredentials.username, userCredentials.password))
            }

            val updateCredentials = updateEmailDomain(userCredentials)
            return userRepository.loginUser(UserRequestModel(updateCredentials.username, updateCredentials.password))
        }
        return Resource.Error("Missing user credentials.")
    }

    suspend fun updateEmailDomain(userCredentials: UserCredentials): UserCredentials {
        val oldEmail = userCredentials.username
        val newEmail = generateUserName()

        val sessionFromLogin = userRepository.loginUser(UserRequestModel(userCredentials.username, userCredentials.password))
        userRepository.updateEmail(newEmail, oldEmail, sessionFromLogin.data!!)
        credentialsStore.deleteUserCredentials()
        val newCredentials = UserCredentials(newEmail, userCredentials.password)
        credentialsStore.storeUserCredentials(newCredentials)

        return newCredentials
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

    private fun isInvalidUserError(resource: Resource<SessionToken>): Boolean {
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
