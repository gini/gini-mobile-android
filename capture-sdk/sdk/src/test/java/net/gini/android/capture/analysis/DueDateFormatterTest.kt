package net.gini.android.capture.analysis

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DueDateFormatterTest {

    @Test
    fun `formats extraction date to local style`() {
        assertThat(DueDateFormatter.formatToLocalStyle("2026-08-13")).isEqualTo("13.08.2026")
    }

    @Test
    fun `formats single digit day and month with leading zeros`() {
        assertThat(DueDateFormatter.formatToLocalStyle("2027-01-05")).isEqualTo("05.01.2027")
    }

    @Test
    fun `falls back to raw value when input is not a date`() {
        assertThat(DueDateFormatter.formatToLocalStyle("not-a-date")).isEqualTo("not-a-date")
    }

    @Test
    fun `falls back to raw value when input uses a different pattern`() {
        assertThat(DueDateFormatter.formatToLocalStyle("13.08.2026")).isEqualTo("13.08.2026")
    }

    @Test
    fun `falls back to raw value when input is empty`() {
        assertThat(DueDateFormatter.formatToLocalStyle("")).isEqualTo("")
    }
}
