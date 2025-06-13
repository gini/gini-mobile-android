package net.gini.android.capture.internal.camera.view.education.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
class EducationMessageColors(
    val text: Color
) {

    companion object {
        @Composable
        fun default(
            text: Color = GiniTheme.colorScheme.text.primary,
        ) = EducationMessageColors(
            text = text,
        )
    }
}
