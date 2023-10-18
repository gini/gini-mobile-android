package net.gini.android.capture.internal.iban

import android.media.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class IBANRecognizerFilter @JvmOverloads constructor(
    private val ibanRecognizer: IBANRecognizer,
    private val listener: Listener,
    listenerContext: CoroutineContext = Dispatchers.Main
) {

    private val listenerScope = CoroutineScope(listenerContext)
    private val ibansFlow = MutableSharedFlow<List<String>>()

    init {
        listenerScope.launch {
            ibansFlow.distinctUntilChanged().collect { ibans ->
                    listener.onIBANsReceived(ibans.setIBANFormatting())
                }
        }
    }

    private fun List<String>.setIBANFormatting(): List<String> {
        return if (this.size == 1) {
            this.map { iban ->
                iban.chunked(4).joinToString(" ")
            }
        } else {
            this
        }
    }

    fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        processingListener: ProcessingListener
    ) {
        ibanRecognizer.processImage(image, width, height, rotationDegrees) {
            processingListener.onProcessingFinished()
            listenerScope.launch {
                ibansFlow.emit(it)
            }
        }
    }

    fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        processingListener: ProcessingListener
    ) {
        ibanRecognizer.processByteArray(byteArray, width, height, rotationDegrees) {
            processingListener.onProcessingFinished()
            listenerScope.launch {
                ibansFlow.emit(it)
            }
        }
    }

    fun cleanup() {
        listenerScope.coroutineContext.cancelChildren()
    }

    interface Listener {
        fun onIBANsReceived(ibans: List<String>)
    }

    interface ProcessingListener {
        fun onProcessingFinished()
    }
}