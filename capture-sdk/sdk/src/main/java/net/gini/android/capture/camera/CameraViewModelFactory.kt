package net.gini.android.capture.camera

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for [CameraViewModel].
 *
 * Internal use only.
 */
internal class CameraViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return CameraViewModel(application) as T
    }
}
