package net.gini.android.bank.sdk.exampleapp.ui.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.GiniComposableStyleProvider
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.components.button.filled.GiniPrimaryButtonStyleConfig

class CustomGiniComposableStyleProvider : GiniComposableStyleProvider {
    @Composable
    override fun setGiniComposableStyleProviderConfig(): GiniComposableStyleProviderConfig? {
        return GiniComposableStyleProviderConfig(
           primaryButtonStyle = GiniPrimaryButtonStyleConfig(
               shape = RoundedCornerShape(3.dp),
               colors = ButtonColors(
                   contentColor = Color.White,
                   containerColor = Color.Magenta,
                   disabledContentColor = Color.Gray,
                   disabledContainerColor = Color.LightGray
               )
           )
        )
    }

}
