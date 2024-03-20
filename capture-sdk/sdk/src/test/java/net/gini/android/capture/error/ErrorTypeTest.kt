package net.gini.android.capture.error

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.internal.util.FileImportValidator
import org.junit.Test
import java.lang.Exception
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import net.gini.android.capture.network.Error as GiniError


class ErrorTypeTest {

    @Test
    fun `typeFromError should return NO_CONNECTION when error cause is UnknownHostException`() {
        // Given
        val error = GiniError("message", UnknownHostException())

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.NO_CONNECTION)
    }

    @Test
    fun `typeFromError should return OUTAGE when error status is 500`() {
        // Given
        val error = GiniError(500, null, null)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.OUTAGE)
    }

    @Test
    fun `typeFromError should return MAINTENANCE when error status is 503`() {
        // Given
        val error = GiniError(503, null, null)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.MAINTENANCE)
    }


    @Test
    fun `typeFromError should return SERVER when error status is 501 and larger excluding 503`() {
        for (i in 501..599) {
            if (i == 503) {
                continue
            }
            // Given
            val error = GiniError(i, null, null)

            // When
            val errorType = ErrorType.typeFromError(error)

            // Then
            assertThat(errorType).isEqualTo(ErrorType.SERVER)
        }
    }


    @Test
    fun `typeFromError should return AUTH when error status is 401`() {
        // Given
        val error = GiniError(401, null, null)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.AUTH)
    }


    @Test
    fun `typeFromError should return AUTH when error status is 400 and error has response body of error invalid_grant`() {

            // Given
            val error = GiniError(400, null, Exception("{\"error\":\"invalid_grant\"}") )

            // When
            val errorType = ErrorType.typeFromError(error)

            // Then
            assertThat(errorType).isEqualTo(ErrorType.AUTH)
    }

    @Test
    fun `typeFromError should return UPLOAD when error status is 400 and larger excluding 401 and 499`() {
        for (i in 400..498) {
            if (i == 401) {
                continue
            }
            // Given
            val error = GiniError(i, null, null)

            // When
            val errorType = ErrorType.typeFromError(error)

            // Then
            assertThat(errorType).isEqualTo(ErrorType.UPLOAD)
        }
    }

    @Test
    fun `typeFromError should return GENERAL when error status is 499`() {
        // Given
        val error = GiniError(499, null, null)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.GENERAL)
    }

    @Test
    fun `typeFromError should return UPLOAD when error status is null and error is SocketTimeoutException`() {
        // Given
        val error = GiniError(null, null, SocketTimeoutException())

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.UPLOAD)
    }


    @Test
    fun `typeFromError should return FILE_IMPORT_SIZE when error status is null and fileImportErrors is SIZE_TOO_LARGE`() {
        // Given
        val error = GiniError(FileImportValidator.Error.SIZE_TOO_LARGE)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_SIZE)
    }

    @Test
    fun `typeFromError should return FILE_IMPORT_UNSUPPORTED when error status is null and fileImportErrors is TYPE_NOT_SUPPORTED`() {
        // Given
        val error = GiniError(FileImportValidator.Error.TYPE_NOT_SUPPORTED)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_UNSUPPORTED)
    }

    @Test
    fun `typeFromError should return FILE_IMPORT_PASSWORD when error status is null and fileImportErrors is PASSWORD_PROTECTED_PDF`() {
        // Given
        val error = GiniError(FileImportValidator.Error.PASSWORD_PROTECTED_PDF)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_PASSWORD)
    }

    @Test
    fun `typeFromError should return FILE_IMPORT_PAGE_COUNT when error status is null and fileImportErrors is TOO_MANY_PDF_PAGES`() {
        // Given
        val error = GiniError(FileImportValidator.Error.TOO_MANY_PDF_PAGES)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_PAGE_COUNT)
    }

    @Test
    fun `typeFromError should return FILE_IMPORT_PAGE_COUNT when error status is null and fileImportErrors is TOO_MANY_DOCUMENT_PAGES`() {
        // Given
        val error = GiniError(FileImportValidator.Error.TOO_MANY_DOCUMENT_PAGES)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_PAGE_COUNT)
    }

    @Test
    fun `typeFromError should return GENERAL when error status is null and fileImportErrors is null`() {
        // Given
        val error = GiniError(null, null, null)

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.GENERAL)
    }

    @Test
    fun `typeFromDocumentErrorCode should return UPLOAD when GiniCaptureDocumentError ErrorCode is UPLOAD_FAILED`() {
        // Given
        val error = GiniCaptureDocumentError.ErrorCode.UPLOAD_FAILED

        // When
        val errorType = ErrorType.typeFromDocumentErrorCode(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.UPLOAD)
    }

    @Test
    fun `typeFromDocumentErrorCode should return FILE_IMPORT_GENERIC when GiniCaptureDocumentError ErrorCode is FILE_VALIDATION_FAILED`() {
        // Given
        val error = GiniCaptureDocumentError.ErrorCode.FILE_VALIDATION_FAILED

        // When
        val errorType = ErrorType.typeFromDocumentErrorCode(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.FILE_IMPORT_GENERIC)
    }


    @Test
    fun `typeFromError should return UPLOAD when status code is 400 and error message is blank - to cover the isInvalidUserError method`() {
        // Given
        val error = GiniError(400,null, Throwable(""))

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.UPLOAD)
    }

    @Test
    fun `typeFromError should return UPLOAD when status code is 400 and error message is not JSON - to cover the isInvalidUserError method`() {
        // Given
        val error = GiniError(400,null, Throwable("sdf"))

        // When
        val errorType = ErrorType.typeFromError(error)

        // Then
        assertThat(errorType).isEqualTo(ErrorType.UPLOAD)
    }

}