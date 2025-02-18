package net.gini.android.capture.ui.compose

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Font Scale 200%",
    group = "FontScale",
    fontScale = 2f,
    showBackground = true,
)
@Preview(
    name = "Font Scale 100%",
    group = "FontScale",
    fontScale = 1f,
    showBackground = true,
)
/**
 * This annotation is used to preview the screen in different sizes (100% and 200% font scale)
 * It can be combined with [GiniScreenPreviewUiModes]
 *
 * @see GiniScreenPreviewUiModes
 */
annotation class GiniScreenPreviewSizes

@Preview(
    name = "Dark",
    group = "UiMode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Light",
    group = "UiMode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
/**
 * This annotation is used to preview the screen in different UI modes (Light and Dark)
 * It can be combined with [GiniScreenPreviewSizes]
 *
 * @see GiniScreenPreviewSizes
 */
annotation class GiniScreenPreviewUiModes
