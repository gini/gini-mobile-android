package net.gini.android.capture.network

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.util.FileImportValidator
import org.junit.Test
import java.net.UnknownHostException

class ErrorTest {

    @Test
    fun `message is the given message`() {
        val error = Error("message", UnknownHostException("cause message"))

        assertThat(error.message).isEqualTo("message")
    }

    @Test
    fun `message is the exception's message when created with status code, headers and exception`() {
        val error = Error(null, null, UnknownHostException("Unable to resolve host"))

        assertThat(error.message).isEqualTo("Unable to resolve host")
    }

    @Test
    fun `message names the exception when the exception has no message`() {
        val error = Error(503, null, UnknownHostException())

        assertThat(error.message).isEqualTo("java.net.UnknownHostException")
    }

    @Test
    fun `message names the status code when there is no exception`() {
        val error = Error(503, null, null)

        assertThat(error.message).isEqualTo("Request failed with status code 503")
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
