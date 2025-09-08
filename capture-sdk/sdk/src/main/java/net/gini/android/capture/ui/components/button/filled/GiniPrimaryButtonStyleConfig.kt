package net.gini.android.capture.ui.components.button.filled

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.ui.graphics.Shape

/**
 * <p>
 *     Use this data class to override the styles of the primary button in the Gini SDK
 *     where Jetpack compose is used. You can override the text in the strings.xml file!
 * </p>
 */
data class GiniPrimaryButtonStyleConfig(
    val shape: Shape? = null,
    val colors: ButtonColors? = null,
    val elevation: ButtonElevation? = null,
    val border: BorderStroke? = null,
)
