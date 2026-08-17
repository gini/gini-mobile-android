package net.gini.android.core.api

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SessionCancellationException
import org.junit.Test
import java.util.concurrent.CancellationException

/**
 * Characterizes the exception to [Resource] mapping in [wrapInResource]. The session
 * interceptor (PP-2363) relies on this mapping to reproduce the same [Resource] shapes for
 * session failures that the repositories returned when they handled tokens themselves.
 */
class ResourceWrapInResourceTest {

    @Test
    fun `maps the request result to success`() = runTest {
        val resource = wrapInResource { "result" }

        assertThat(resource).isEqualTo(Resource.Success("result"))
    }

    @Test
    fun `maps ApiException to an error resource with status code, body, headers and parsed error response`() = runTest {
        val resource: Resource<String> = wrapInResource {
            throw ApiException(
                message = "user authentication failed",
                responseStatusCode = 401,
                responseBody = """{"message":"invalid session","requestId":"request-id-99"}""",
                responseHeaders = mapOf("www-authenticate" to listOf("Bearer"))
            )
        }

        val error = resource as Resource.Error
        assertThat(error.message).isEqualTo("user authentication failed")
        assertThat(error.responseStatusCode).isEqualTo(401)
        assertThat(error.responseBody).isEqualTo("""{"message":"invalid session","requestId":"request-id-99"}""")
        assertThat(error.responseHeaders).isEqualTo(mapOf("www-authenticate" to listOf("Bearer")))
        assertThat(error.exception).isInstanceOf(ApiException::class.java)
        assertThat(error.errorResponse?.message).isEqualTo("invalid session")
        assertThat(error.errorResponse?.requestId).isEqualTo("request-id-99")
    }

    @Test
    fun `maps SessionCancellationException to a cancelled resource`() = runTest {
        val resource: Resource<String> = wrapInResource { throw SessionCancellationException() }

        assertThat(resource).isInstanceOf(Resource.Cancelled::class.java)
    }

    @Test
    fun `maps CancellationException to a cancelled resource`() = runTest {
        val resource: Resource<String> = wrapInResource { throw CancellationException() }

        assertThat(resource).isInstanceOf(Resource.Cancelled::class.java)
    }

    @Test
    fun `maps other exceptions to an error resource with the exception message`() = runTest {
        val exception = IllegalStateException("something broke")
        val resource: Resource<String> = wrapInResource { throw exception }

        val error = resource as Resource.Error
        assertThat(error.message).isEqualTo("something broke")
        assertThat(error.exception).isSameInstanceAs(exception)
        assertThat(error.responseStatusCode).isNull()
    }
}
