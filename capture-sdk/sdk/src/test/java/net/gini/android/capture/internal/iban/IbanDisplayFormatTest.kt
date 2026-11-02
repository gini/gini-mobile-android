package net.gini.android.capture.internal.iban

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IbanDisplayFormatTest {

    @Test
    fun `groups a compact IBAN in blocks of four`() {
        assertThat(formatIbanForDisplay("DE89370400440532013000"))
            .isEqualTo("DE89 3704 0044 0532 0130 00")
    }

    @Test
    fun `re-groups an IBAN that already contains spaces`() {
        assertThat(formatIbanForDisplay("PL12 1000 0111 0100 1000 1234 5678"))
            .isEqualTo("PL12 1000 0111 0100 1000 1234 5678")
        assertThat(formatIbanForDisplay("PL 12100001 110100100012345678"))
            .isEqualTo("PL12 1000 0111 0100 1000 1234 5678")
    }

    @Test
    fun `keeps an IBAN whose length is a multiple of four without a trailing space`() {
        assertThat(formatIbanForDisplay("AT611904300234573201"))
            .isEqualTo("AT61 1904 3002 3457 3201")
    }

    @Test
    fun `returns an empty string for an empty input`() {
        assertThat(formatIbanForDisplay("")).isEmpty()
        assertThat(formatIbanForSpeech("")).isEmpty()
    }

    @Test
    fun `speech form separates every character and pauses between blocks`() {
        assertThat(formatIbanForSpeech("DE89370400440532013000"))
            .isEqualTo("D E 8 9, 3 7 0 4, 0 0 4 4, 0 5 3 2, 0 1 3 0, 0 0")
        assertThat(formatIbanForSpeech("AT61 1904 3002 3457 3201"))
            .isEqualTo("A T 6 1, 1 9 0 4, 3 0 0 2, 3 4 5 7, 3 2 0 1")
    }
}
