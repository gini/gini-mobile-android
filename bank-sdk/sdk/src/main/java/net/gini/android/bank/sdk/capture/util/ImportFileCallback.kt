package net.gini.android.bank.sdk.capture.util

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.bank.sdk.capture.CaptureImportInput

internal fun getImportFileCallback(resultLauncher: ActivityResultLauncher<CaptureImportInput>) =
    object : AsyncCallback<Intent, ImportedFileValidationException> {
        override fun onSuccess(result: Intent) {
            resultLauncher.launch(CaptureImportInput.Forward(result))
        }

        override fun onError(exception: ImportedFileValidationException?) {
            resultLauncher.launch(CaptureImportInput.Error(exception?.validationError, exception?.message))
        }

        override fun onCancelled() {
        }
    }