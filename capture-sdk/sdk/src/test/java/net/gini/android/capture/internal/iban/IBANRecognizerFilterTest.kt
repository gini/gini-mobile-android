package net.gini.android.capture.internal.iban

import android.media.Image
import com.nhaarman.mockitokotlin2.any
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
        advanceTimeBy(1)

        // Then
        verify(listenerSpy).onIBANsReceived(eq(listOf("1")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("2")))

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `apply format to a single iban`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(
                listOf(
                    listOf("NL60ABNA8181091612"),
                    listOf("DE83500105175744527463"),
                    listOf("DE96500105175969721944"),
                    listOf("DE76500105172435532833"),
                    listOf("DE95500105172898696724")
                )
            ), listenerSpy, this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        advanceTimeBy(1)

        // Then
        verify(listenerSpy).onIBANsReceived(eq(listOf("NL60 ABNA 8181 0916 12")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("DE83 5001 0517 5744 5274 63")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("DE96 5001 0517 5969 7219 44")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("DE76 5001 0517 2435 5328 33")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("DE95 5001 0517 2898 6967 24")))

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `do not apply format to multiple ibans`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(
                listOf(
                    listOf("NL60ABNA8181091612", "DE83500105175744527463"),
                    listOf("DE96500105175969721944", "DE76500105172435532833", "DE95500105172898696724")
                )
            ), listenerSpy, this.coroutineContext
        )

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, noOpProcessingListener)
        advanceTimeBy(1)

        // Then
        verify(listenerSpy).onIBANsReceived(eq(listOf("NL60ABNA8181091612", "DE83500105175744527463")))
        verify(listenerSpy).onIBANsReceived(eq(listOf("DE96500105175969721944", "DE76500105172435532833", "DE95500105172898696724")))

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `informs processing finished via listener`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(listOf(listOf("1"), emptyList(), listOf("2"))),
            listenerSpy,
            this.coroutineContext
        )
        val processingListenerSpy: IBANRecognizerFilter.ProcessingListener = spy()

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)

        // Then
        verify(processingListenerSpy, times(3)).onProcessingFinished()

        ibanRecognizerFilter.cleanup()
    }

    @Test
    fun `informs processing finished via listener also when processing was cancelled`() = runTest {
        // Given
        val listenerSpy: IBANRecognizerFilter.Listener = spy()
        val ibanRecognizerFilter = IBANRecognizerFilter(
            IBANRecognizerStub(expectedValues = emptyList(), isCancelled = true),
            listenerSpy,
            this.coroutineContext
        )
        val processingListenerSpy: IBANRecognizerFilter.ProcessingListener = spy()

        // When
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)
        ibanRecognizerFilter.processImage(mock(), 200, 300, 0, processingListenerSpy)

        // Then
        verify(processingListenerSpy, times(3)).onProcessingFinished()

        ibanRecognizerFilter.cleanup()
    }

    class IBANRecognizerStub(
        private val expectedValues: List<List<String>>,
        private val isCancelled: Boolean = false
    ) : IBANRecognizer {

        private var counter = 0

        override fun processImage(
            image: Image,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (List<String>) -> Unit,
            cancelledCallback: () -> Unit
        ) {
            if (isCancelled) {
                cancelledCallback()
            } else if (expectedValues.isNotEmpty()) {
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
            doneCallback: (List<String>) -> Unit,
            cancelledCallback: () -> Unit
        ) {
            if (isCancelled) {
                cancelledCallback()
            } else if (expectedValues.isNotEmpty()) {
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