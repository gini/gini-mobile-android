package net.gini.android.capture.ui.theme.modifier

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.max
import net.gini.android.capture.R

@Composable
fun Modifier.tabletMaxWidth(): Modifier {
    return if (isTablet()) {
        widthIn(max = dimensionResource(id = R.dimen.gc_tablet_width))
    } else {
        fillMaxWidth()
    }
}

@Composable
private fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        configuration.screenWidthDp > 840
    } else {
        configuration.screenWidthDp > 600
    }
}