package net.gini.android.internal.payment.utils

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.utils.extensions.isValidPdfName
import org.junit.Test


class PdfNameValidatorTest {

    @Test
    fun `empty pdf name is not valid`() = runTest {
        Truth.assertThat("".isValidPdfName()).isFalse()
    }

    @Test
    fun `pdf name with more than 25 characters is not valid`() = runTest {
        Truth.assertThat("this-pdf-name-has-more-than-25-characters".isValidPdfName()).isFalse()
    }

    @Test
    fun `pdf name with _ is valid`() = runTest {
        Truth.assertThat("pdf_name".isValidPdfName()).isTrue()
    }

    @Test
    fun `pdf name with - is valid`() = runTest {
        Truth.assertThat("pdf-name".isValidPdfName()).isTrue()
    }

    @Test
    fun `pdf name with _ and - is valid`() = runTest {
        Truth.assertThat("pdf_name-underscore".isValidPdfName()).isTrue()
    }

    @Test
    fun `pdf names with other special characters than - or _ are not valid`() = runTest {
        Truth.assertThat("pdf name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf!name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf+name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf/name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf&name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf%name".isValidPdfName()).isFalse()
        Truth.assertThat("pdf.name".isValidPdfName()).isFalse()
    }

    @Test
    fun `pdf names with numbers are valid`() = runTest {
        Truth.assertThat("123pdf_name".isValidPdfName()).isTrue()
        Truth.assertThat("pdf_name123".isValidPdfName()).isTrue()
        Truth.assertThat("pdf123_123name".isValidPdfName()).isTrue()
        Truth.assertThat("p1d2f_n3m4".isValidPdfName()).isTrue()
    }

    @Test
    fun `pdf names without numbers are valid`() = runTest {
        Truth.assertThat("pdf_name".isValidPdfName()).isTrue()
    }
}