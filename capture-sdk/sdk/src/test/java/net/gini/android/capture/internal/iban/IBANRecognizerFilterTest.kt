package net.gini.android.capture.internal.iban

import android.media.Image
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IBANRecognizerFilterTest {

    private val noOpProcessingListener = object : IBANRecognizerFilter.ProcessingListener {
        override fun onProcessingFinished() {
        }
    }

    @Test
    fun `filters out duplicate callback values for image`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(emptyList(), emptyList(), emptyList(), emptyList())),
            listenerSpy,
            this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        advanceTimeBy(1)

        // Then
        verify(listenerSpy, times(1)).onIBANsReceived(any())

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `filters out duplicate callback values for byte arrays`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(emptyList(), emptyList(), emptyList(), emptyList())),
            listenerSpy,
            this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processByteArray(ByteArray(0), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processByteArray(ByteArray(0), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processByteArray(ByteArray(0), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processByteArray(ByteArray(0), 200, 300, 0, noOpProcessingListener)
        advanceTimeBy(1)

        // Then
        verify(listenerSpy, times(1)).onIBANsReceived(any())

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `returns the callback value when it changes for images`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(IBANRecognizerStub(listOf(listOf("1"), listOf("2"))), listenerSpy, this.coroutineContext)

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        advanceTimeBy(1010)

        // Then
        verify(listenerSpy).onIBANsReceived(eq(listOf("1")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("2")))

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `delays subsequent non-empty callback values by one second for image`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(listOf("1"), listOf("2"), listOf("3"))),
            listenerSpy,
            this.coroutineContext
        )

        // When - emitting two non-empty callback values
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)

        // Then - the first one is returned immediately and the second one after one second
        advanceTimeBy(1)
        verify(listenerSpy).onIBANsReceived(listOf("1"))
        advanceTimeBy(900)
        verify(listenerSpy, times(0)).onIBANsReceived(listOf("2"))
        advanceTimeBy(110)
        verify(listenerSpy).onIBANsReceived(listOf("2"))

        // When - emitting the third non-empty callback value
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)

        // Then - it is returned after one second
        advanceTimeBy(900)
        verify(listenerSpy, times(0)).onIBANsReceived(listOf("3"))
        advanceTimeBy(110)
        verify(listenerSpy).onIBANsReceived(listOf("3"))

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `returns empty callback values without delay for image`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(listOf("1"), emptyList(), emptyList())),
            listenerSpy,
            this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)

        // Then - both are returned without delay
        advanceTimeBy(1)
        verify(listenerSpy, atMost(1)).onIBANsReceived(listOf("1"))
        verify(listenerSpy, atMost(1)).onIBANsReceived(emptyList())

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `returns non-empty callback value after empty callback value without delay for image`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(listOf("1"), emptyList(), listOf("2"))),
            listenerSpy,
            this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)

        // Then
        advanceTimeBy(1)
        verify(listenerSpy).onIBANsReceived(listOf("1"))
        verify(listenerSpy).onIBANsReceived(emptyList())
        verify(listenerSpy).onIBANsReceived(listOf("2"))

        ibanRecognizerFilter.cleanup()
    }

    class IBANRecognizerStub(
        private val expectedValues: List<List<String>>
    ) : IBANRecognizer {

        private var counter = 0

        override fun processImage(
            image: Image,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (List<String>) -> Unit
        ) {
            if (expectedValues.isNotEmpty()) {
                doneCallback(expectedValues[counter])
                counter++
            } else {
                doneCallback(emptyList())
            }
        }

        override fun processByteArray(
            byteArray: ByteArray,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (List<String>) -> Unit
        ) {
            if (expectedValues.isNotEmpty()) {
                doneCallback(expectedValues[counter])
                counter++
            } else {
                doneCallback(emptyList())
            }
        }

        override fun close() {
        }
    }

}