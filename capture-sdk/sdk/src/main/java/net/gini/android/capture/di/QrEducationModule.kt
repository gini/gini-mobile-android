package net.gini.android.capture.di

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.internal.storage.QrCodeEducationStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val qrEducationModule = module {
    single {
        QrCodeEducationStorage(
            context = androidContext()
        )
    }
    factory {
        GetQrEducationTypeUseCase(
            qrCodeEducationStorage = get(),
            isOnlyQrCodeScanningEnabledProvider = {
                runCatching { GiniCapture.getInstance().isOnlyQRCodeScanning }.getOrNull()
            },
            documentImportEnabledFileTypesProvider = {
                runCatching { GiniCapture.getInstance().documentImportEnabledFileTypes }.getOrNull()
            },
            flowTypeStorage = get()
        )
    }
    factory {
        IncrementQrCodeRecognizedCounterUseCase(
            qrCodeEducationStorage = get()
        )
    }
}
