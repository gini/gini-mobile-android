package net.gini.android.core.api.authorization

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserRemoteSourceTest {


    // region Bearer authorization header tests

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in createUser`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "Bearer $accessToken"
        verifyHeader(name = "Authorization", value = expectedHeaderValue, testScope = this) {
            createUser(UserRequestModel(), accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getGiniApiSessionTokenInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "Bearer $accessToken"
        verifyHeader(name = "Authorization", value = expectedHeaderValue, testScope = this) {
            getGiniApiSessionTokenInfo("", accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getUserInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "Bearer $accessToken"
        verifyHeader(name = "Authorization", value = expectedHeaderValue, testScope = this) {
            getUserInfo("", accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in updateEmail`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "Bearer $accessToken"
        verifyHeader(name = "Authorization", value = expectedHeaderValue, testScope = this) {
            updateEmail("", UserRequestModel(), accessToken)
        }
    }

    // endregion

    // region Basic authorization header tests
    @Test
    fun `sets basic authorization header with capital case 'Basic' in signIn`() = runTest {
        val clientId = "id"
        val clientSecret = "secret"
        val credentials = Base64.encodeToString("${clientId}:${clientSecret}".toByteArray(), Base64.NO_WRAP)
        val expectedHeaderValue = "Basic $credentials"
        verifyHeader(
            name = "Authorization",
            value = expectedHeaderValue,
            clientId = clientId,
            clientSecret = clientSecret,
            testScope = this
        ) {
            signIn(UserRequestModel())
        }
    }

    @Test
    fun `sets basic authorization header with capital case 'Basic' in logInClient`() = runTest {
        val clientId = "id"
        val clientSecret = "secret"
        val credentials = Base64.encodeToString("${clientId}:${clientSecret}".toByteArray(), Base64.NO_WRAP)
        val expectedHeaderValue = "Basic $credentials"
        verifyHeader(
            name = "Authorization",
            value = expectedHeaderValue,
            clientId = clientId,
            clientSecret = clientSecret,
            testScope = this
        ) {
            loginClient()
        }
    }

    // endregion

    // region Accept header tests

    @Test
    fun `sets json accept header in createUser`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "application/json"
        verifyHeader(name = "Accept", value = expectedHeaderValue, testScope = this) {
            createUser(UserRequestModel(), accessToken)
        }
    }

    @Test
    fun `sets json accept header in getGiniApiSessionTokenInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "application/json"
        verifyHeader(name = "Accept", value = expectedHeaderValue, testScope = this) {
            getGiniApiSessionTokenInfo("", accessToken)
        }
    }

    @Test
    fun `sets json accept header in getUserInfo`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "application/json"
        verifyHeader(name = "Accept", value = expectedHeaderValue, testScope = this) {
            getUserInfo("", accessToken)
        }
    }

    @Test
    fun `sets json accept header in updateEmail`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedHeaderValue = "application/json"
        verifyHeader(name = "Accept", value = expectedHeaderValue, testScope = this) {
            updateEmail("", UserRequestModel(), accessToken)
        }
    }

    @Test
    fun `sets json accept header in signIn`() = runTest {
        val clientId = "id"
        val clientSecret = "secret"
        val credentials = Base64.encodeToString("${clientId}:${clientSecret}".toByteArray(), Base64.NO_WRAP)
        val expectedHeaderValue = "application/json"
        verifyHeader(
            name = "Accept",
            value = expectedHeaderValue,
            clientId = clientId,
            clientSecret = clientSecret,
            testScope = this
        ) {
            signIn(UserRequestModel())
        }
    }

    @Test
    fun `sets json accept header in logInClient`() = runTest {
        val clientId = "id"
        val clientSecret = "secret"
        val credentials = Base64.encodeToString("${clientId}:${clientSecret}".toByteArray(), Base64.NO_WRAP)
        val expectedHeaderValue = "application/json"
        verifyHeader(
            name = "Accept",
            value = expectedHeaderValue,
            clientId = clientId,
            clientSecret = clientSecret,
            testScope = this
        ) {
            loginClient()
        }
    }

    // endregion

    private inline fun verifyHeader(
        name: String,
        value: String,
        clientId: String = "",
        clientSecret: String = "",
        testScope: TestScope,
        testBlock: UserRemoteSource.() -> Unit
    ) {
        // Given
        val userServiceInterceptor = UserServiceInterceptor()
        val testSubject =
            UserRemoteSource(
                StandardTestDispatcher(testScope.testScheduler),
                userServiceInterceptor,
                clientId,
                clientSecret
            )

        // When
        with(testSubject) {
            testBlock()
        }
        testScope.advanceUntilIdle()

        // Then
        Truth.assertThat(userServiceInterceptor.headers[name]).isNotNull()
        Truth.assertThat(userServiceInterceptor.headers[name]).isEqualTo(value)
    }

    private class UserServiceInterceptor : UserService {

        var headers: Map<String, String> = emptyMap()

        override suspend fun signIn(
            basicAuthHeaders: Map<String, String>,
            userName: String,
            password: String
        ): Response<SessionToken> {
            headers = basicAuthHeaders
            return Response.success(SessionToken("", "", 0))
        }

        override suspend fun loginClient(basicAuthHeaders: Map<String, String>): Response<SessionToken> {
            headers = basicAuthHeaders
            return Response.success(SessionToken("", "", 0))
        }

        override suspend fun createUser(
            bearerHeaders: Map<String, String>,
            userRequestModel: UserRequestModel
        ): Response<ResponseBody> {
            headers = bearerHeaders
            return Response.success(null)
        }

        override suspend fun getGiniApiSessionTokenInfo(
            bearerHeaders: Map<String, String>,
            token: String
        ): Response<SessionTokenInfo> {
            headers = bearerHeaders
            return Response.success(SessionTokenInfo(""))
        }

        override suspend fun getUserInfo(bearerHeaders: Map<String, String>, uri: String): Response<UserResponseModel> {
            headers = bearerHeaders
            return Response.success(UserResponseModel())
        }

        override suspend fun updateEmail(
            bearerHeaders: Map<String, String>,
            userId: String,
            userRequestModel: UserRequestModel
        ): Response<ResponseBody> {
            headers = bearerHeaders
            return Response.success(null)
        }

    }
}