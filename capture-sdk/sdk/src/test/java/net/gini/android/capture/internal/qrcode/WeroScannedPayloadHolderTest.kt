package net.gini.android.capture.internal.qrcode

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class WeroScannedPayloadHolderTest {

    @After
    fun tearDown() {
        // The holder is a process-wide object; drain it after every test so one case cannot
        // leak state into the next.
        WeroScannedPayloadHolder.consume()
    }

    @Test
    fun `consume returns null in the initial state`() {
        assertThat(WeroScannedPayloadHolder.consume()).isNull()
    }

    @Test
    fun `consume returns the previously set payload`() {
        WeroScannedPayloadHolder.set("payload")

        assertThat(WeroScannedPayloadHolder.consume()).isEqualTo("payload")
    }

    @Test
    fun `a second consume after a single set returns null`() {
        WeroScannedPayloadHolder.set("payload")
        WeroScannedPayloadHolder.consume()

        assertThat(WeroScannedPayloadHolder.consume()).isNull()
    }

    @Test
    fun `a second set overwrites the first unconsumed value`() {
        WeroScannedPayloadHolder.set("first")
        WeroScannedPayloadHolder.set("second")

        assertThat(WeroScannedPayloadHolder.consume()).isEqualTo("second")
    }
}
