package net.gini.android.bank.sdk.transactionlist.ui.extractions

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.transactionlist.ui.extractions.colors.ExtractionResultDocumentsSectionColors
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenu
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenuItem
import net.gini.android.capture.ui.theme.GiniTheme

private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif")

@Composable
fun ExtractionResultDocumentSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier,
        shape = RectangleShape
    ) {
        Column {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(id = R.string.gbs_tl_extraction_result_documents_section_title),
                style = GiniTheme.typography.subtitle2,
            )
            Column(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun ExtractionResultDocumentSection(
    modifier: Modifier = Modifier,
) {
    val dummyDocuments = listOf("IMG_20240807_075215616516515645664333.jpg", "Rechnung-223.pdf")

    ExtractionResultDocumentSection(
        modifier = modifier.fillMaxWidth(),
        content = {
            dummyDocuments.forEach {
                Document(documentName = it)
            }
        }
    )
}

@Composable
private fun Document(
    documentName: String,
    modifier: Modifier = Modifier,
    colors: ExtractionResultDocumentsSectionColors.DocumentItemColors =
        ExtractionResultDocumentsSectionColors.DocumentItemColors.colors(),
) {

    var menuVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DocumentImage(
                imageUrl = null,
                documentName = documentName,
            )
            Text(
                modifier = Modifier
                    .weight(0.1f)
                    .padding(horizontal = 16.dp),
                text = documentName,
                style = GiniTheme.typography.subtitle1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = colors.textColor,
            )
            Box {
                Icon(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            menuVisible = true
                        },
                    painter = painterResource(id = R.drawable.gbs_more_horizontal),
                    tint = colors.moreIconTint,
                    contentDescription = null
                )
                if (menuVisible) {
                    GiniDropdownMenu(
                        colors = colors.menuColors,
                        expanded = true,
                        onDismissRequest = { menuVisible = false },
                    ) {
                        DocumentMenuItem(
                            modifier = Modifier.align(Alignment.End),
                            onClick = { },
                            title = stringResource(id = R.string.gbs_tl_extraction_result_documents_section_menu_delete)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentMenuItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    title: String,
    colors: ExtractionResultDocumentsSectionColors.DocumentItemColors =
        ExtractionResultDocumentsSectionColors.DocumentItemColors.colors(),
) {
    GiniDropdownMenuItem(
        modifier = modifier,
        text = {
            Text(
                text = title,
                style = GiniTheme.typography.body1,
                color = colors.textColor
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.gbs_delete),
                contentDescription = null,
                tint = colors.textColor
            )
        },
        onClick = onClick
    )
}

@Composable
private fun DocumentImage(
    imageUrl: String?,
    documentName: String,
    modifier: Modifier = Modifier,
    colorScheme: ExtractionResultDocumentsSectionColors.DocumentItemColors.IconPlaceholderColors =
        ExtractionResultDocumentsSectionColors.DocumentItemColors.IconPlaceholderColors.colors(),
) {
    if (imageUrl != null) {
        // Place Async Image here in future
    } else {
        val iconResId = if (imageExtensions.find { documentName.endsWith(it, true) } != null) {
            R.drawable.gbs_tl_document_placeholder_image
        } else {
            R.drawable.gbs_tl_document_placeholder_file
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .padding(8.dp)
                .background(colorScheme.iconBackgroundColor, shape = RoundedCornerShape(4.dp))
        ) {
            Icon(
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp),
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = colorScheme.iconTint,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExtractionResultDocumentsSectionPreview() {
    GiniTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExtractionResultDocumentSection()
        }
    }
}

@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun ExtractionResultDocumentsSectionPreviewDark() {
    GiniTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExtractionResultDocumentSection()
        }
    }
}
