@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.ui.components.topbar

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: GiniTopBarColors = GiniTopBarColors.colors(),
) {
    GiniTopBar(
        modifier = modifier, colors = colors, title = {
            Text(
                modifier = Modifier.padding(16.dp),
                text = title,
                style = GiniTheme.typography.headline6,
            )
        }, navigationIcon = navigationIcon, actions = actions
    )
}

@Composable
fun GiniTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    colors: GiniTopBarColors = GiniTopBarColors.colors(),
) {
    TopAppBar(
        modifier = modifier, colors = with(colors) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = contentColor,
                navigationIconContentColor = contentColor,
                actionIconContentColor = contentColor,
            )
        }, title = title, navigationIcon = navigationIcon, actions = actions
    )
}


@Preview
@Composable
private fun GiniTopBarPreviewLight() {
    GiniTopBarPreview()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GiniTopBarPreviewDark() {
    GiniTopBarPreview()
}

@Composable
private fun GiniTopBarPreview() {
    GiniTheme {
        GiniTopBar(title = "Title", navigationIcon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.ArrowBack),
                contentDescription = null,
            )
        }, actions = {
            Icon(
                modifier = Modifier.padding(16.dp),
                painter = painterResource(net.gini.android.capture.R.drawable.gc_help_icon),
                contentDescription = null,
            )
        })
    }
}