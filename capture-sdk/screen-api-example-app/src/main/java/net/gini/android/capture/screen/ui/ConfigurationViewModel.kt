package net.gini.android.capture.screen.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.capture.screen.ui.data.Configuration
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(): ViewModel() {

    private val _configuration = MutableStateFlow(Configuration())
    val configuration: StateFlow<Configuration> = _configuration

    fun setConfiguration(configuration: Configuration) {
        _configuration.value = configuration
    }

}