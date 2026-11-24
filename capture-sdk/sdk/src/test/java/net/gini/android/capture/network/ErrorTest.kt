package net.gini.android.capture.network

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.util.FileImportValidator
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorTest {

    @Test
    fun `message is the given message`() {
        val error = Error("message", UnknownHostException("cause message"))

        assertThat(error.message).isEqualTo("message")
    }

    @Test
    fun `message names the status code when created with a status code`() {
        val error = Error(503, null, null)

        assertThat(error.message).isEqualTo("Request failed with status code 503")
    }

    @Test
    fun `message names the exception's type when there is no status code`() {
        val error = Error(null, null, UnknownHostException("Unable to resolve host"))

        assertThat(error.message).isEqualTo("Request failed: java.net.UnknownHostException")
    }

    @Test
    fun `message does not contain the exception's message`() {
        // For API errors the exception's message is the response body
        val responseBody = """{"message":"Invalid document","requestId":"1234"}"""

        val withStatusCode = Error(400, null, IOException(responseBody))
        val withoutStatusCode = Error(null, null, IOException(responseBody))

        assertThat(withStatusCode.message).isEqualTo("Request failed with status code 400")
        assertThat(withoutStatusCode.message).isEqualTo("Request failed: java.io.IOException")
    }

    @Test
    fun `message is generic when there is neither a status code nor an exception`() {
        val error = Error(null, null, null)

        assertThat(error.message).isEqualTo("Request failed")
    }

    @Test
    fun `message names the file import error`() {
        val error = Error(FileImportValidator.Error.SIZE_TOO_LARGE)

        assertThat(error.message).isEqualTo("File import failed: SIZE_TOO_LARGE")
    }
}
