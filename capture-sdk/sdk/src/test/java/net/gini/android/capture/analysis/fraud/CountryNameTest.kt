package net.gini.android.capture.analysis.fraud

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class CountryNameTest {

    @Test
    fun `returns the German country name for a German locale`() {
        assertThat(countryDisplayName("PL", Locale.GERMAN)).isEqualTo("Polen")
    }

    @Test
    fun `returns the English country name for an English locale and a lower-case code`() {
        assertThat(countryDisplayName("at", Locale.ENGLISH)).isEqualTo("Austria")
    }

    @Test
    fun `returns the code itself for an unknown country`() {
        assertThat(countryDisplayName("XQ", Locale.ENGLISH)).isEqualTo("XQ")
    }
}
