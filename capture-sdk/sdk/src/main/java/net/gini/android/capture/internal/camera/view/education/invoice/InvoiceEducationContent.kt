package net.gini.android.capture.internal.camera.view.education.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.gini.android.capture.R
import net.gini.android.capture.internal.camera.view.education.AnimatedEducationMessageWithIntro
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun InvoiceEducationContent(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .fillMaxSize()
    ) {
        AnimatedEducationMessageWithIntro(
            modifier = modifier.align(Alignment.Center),
            message = stringResource(R.string.gc_invoice_education_message),
            imagePainter = painterResource(R.drawable.gc_invoice_education_upload_picture_image),
            onComplete = onComplete,
            animationKey = Unit
        )
    }
}

@Composable
@Preview
private fun QrCodeEducationPopupPreview(
) {
    GiniTheme {
        InvoiceEducationContent(
            onComplete = { /* no-op */ }
        )
    }
}
