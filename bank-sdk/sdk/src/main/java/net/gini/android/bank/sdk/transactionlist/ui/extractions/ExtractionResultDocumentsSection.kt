package net.gini.android.bank.sdk.transactionlist.ui.extractions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.transactionlist.ui.extractions.colors.ExtractionResultDocumentsSectionColors
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
        modifier = modifier,
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
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentImage(
            imageUrl = null,
            documentName = documentName,
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = documentName,
            style = GiniTheme.typography.subtitle1,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
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
        // TODO
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

@Preview
@Composable
private fun ExtractionResultDocumentsSectionPreview() {
    GiniTheme {
        ExtractionResultDocumentSection()
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExtractionResultDocumentsSectionPreviewDark() {
    GiniTheme {
        ExtractionResultDocumentSection()
    }
}
