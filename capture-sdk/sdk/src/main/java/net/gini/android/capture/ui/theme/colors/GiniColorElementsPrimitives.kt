package net.gini.android.capture.ui.theme.colors

import android.content.Context
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.R

data class GiniColorElementsPrimitives(
    val skontoSectionTitleTextColor: Color = Color(0xFF161616),

) {
    companion object {
        /**
         * Bridge between old way of defining colors by overridden resources and Compose
         *
         * This function will define color primitives based on resources value at res/colors.xml
         */
        internal fun buildColorPrimitivesBasedOnResources(context: Context) = GiniColorElementsPrimitives(
            skontoSectionTitleTextColor = Color(context.getColor(R.color.skonto_section_title_text_color)),
        )
    }
}