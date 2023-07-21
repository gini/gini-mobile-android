package net.gini.android.core.api.authorization

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.SessionTokenInfo
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class UserRemoteSourceTest {

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in createUser`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            createUser(UserRequestModel(), accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getGiniApiSessionTokenInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getGiniApiSessionTokenInfo("", accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getUserInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getUserInfo("", accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in updateEmail`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            updateEmail("", UserRequestModel(), accessToken)
        }
    }

    private inline fun verifyAuthorizationHeader(
        expectedAuthorizationHeader: String,
        testScope: TestScope,
        testBlock: UserRemoteSource.() -> Unit
    ) {
        // Given
        val userServiceAuthInterceptor = UserServiceAuthInterceptor()
        val testSubject =
            UserRemoteSource(StandardTestDispatcher(testScope.testScheduler), userServiceAuthInterceptor, "", "")

        // When
        with(testSubject) {
            testBlock()
        }
        testScope.advanceUntilIdle()

        // Then
        Truth.assertThat(userServiceAuthInterceptor.bearerAuthHeader).isNotNull()
        Truth.assertThat(userServiceAuthInterceptor.bearerAuthHeader).isEqualTo(expectedAuthorizationHeader)
    }

    private class UserServiceAuthInterceptor : UserService {

        var bearerAuthHeader: String? = null

        override suspend fun signIn(
            basicAuthHeaders: Map<String, String>,
            userName: String,
            password: String
        ): Response<SessionToken> {
            return Response.success(null)
        }

        override suspend fun loginClient(basicAuthHeaders: Map<String, String>): Response<SessionToken> {
            return Response.success(null)
        }

        override suspend fun createUser(
            bearerHeaders: Map<String, String>,
            userRequestModel: UserRequestModel
        ): Response<ResponseBody> {
            bearerAuthHeader = bearerHeaders["Authorization"]
            return Response.success(null)
        }

        override suspend fun getGiniApiSessionTokenInfo(
            bearerHeaders: Map<String, String>,
            token: String
        ): Response<SessionTokenInfo> {
            bearerAuthHeader = bearerHeaders["Authorization"]
            return Response.success(SessionTokenInfo(""))
        }

        override suspend fun getUserInfo(bearerHeaders: Map<String, String>, uri: String): Response<UserResponseModel> {
            bearerAuthHeader = bearerHeaders["Authorization"]
            return Response.success(UserResponseModel())
        }

        override suspend fun updateEmail(
            bearerHeaders: Map<String, String>,
            userId: String,
            userRequestModel: UserRequestModel
        ): Response<ResponseBody> {
            bearerAuthHeader = bearerHeaders["Authorization"]
            return Response.success(null)
        }

    }
}