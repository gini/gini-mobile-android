package net.gini.android.capture.ui.theme.modifier

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.dimensionResource
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
private fun isTablet(): Boolean = booleanResource(id = R.bool.gc_is_tablet)