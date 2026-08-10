package net.gini.android.capture.internal.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UnsupportedQrWarningSessionPinTest {

    @Test
    fun `first pinned value wins for the whole session`() {
        val pin = UnsupportedQrWarningSessionPin()

        assertThat(pin.pinIfAbsent { true }).isTrue()
        // A later, different value must not change the session decision
        assertThat(pin.pinIfAbsent { false }).isTrue()
    }

    @Test
    fun `compute is not invoked once a value is pinned`() {
        val pin = UnsupportedQrWarningSessionPin()
        pin.pinIfAbsent { false }

        var computed = false
        pin.pinIfAbsent { computed = true; true }

        assertThat(computed).isFalse()
    }

    @Test
    fun `reset allows the next session to pin a new value`() {
        val pin = UnsupportedQrWarningSessionPin()
        pin.pinIfAbsent { true }

        pin.reset()

        assertThat(pin.pinIfAbsent { false }).isFalse()
    }

    @Test
    fun `concurrent callers all observe the same pinned value`() {
        val pin = UnsupportedQrWarningSessionPin()
        val threads = 8
        val executor = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        try {
            val results = (0 until threads).map { i ->
                executor.submit<Boolean> {
                    ready.countDown()
                    start.await()
                    pin.pinIfAbsent { i % 2 == 0 }
                }
            }
            ready.await()
            start.countDown()

            val values = results.map { it.get(5, TimeUnit.SECONDS) }.toSet()
            assertThat(values).hasSize(1)
        } finally {
            executor.shutdown()
        }
    }
}
