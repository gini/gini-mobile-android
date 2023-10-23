package net.gini.android.capture.internal.iban

import android.media.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
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
                    listener.onIBANsReceived(setIBANFormatting(ibans))
                }
        }
    }

    private fun setIBANFormatting(ibans: List<String>): List<String> {
        return if (ibans.size == 1) {
            ibans.map { iban ->
                iban.chunked(4).joinToString(" ")
            }
        } else {
            ibans
        }
    }

    fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        processingListener: ProcessingListener
    ) {
        ibanRecognizer.processImage(image, width, height, rotationDegrees, doneCallback =  {
            processingListener.onProcessingFinished()
            listenerScope.launch {
                ibansFlow.emit(it)
            }
        }, cancelledCallback = {
            processingListener.onProcessingFinished()
        })
    }

    fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        processingListener: ProcessingListener
    ) {
        ibanRecognizer.processByteArray(byteArray, width, height, rotationDegrees, doneCallback =  {
            processingListener.onProcessingFinished()
            listenerScope.launch {
                ibansFlow.emit(it)
            }
        }, cancelledCallback = {
            processingListener.onProcessingFinished()
        })
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