package net.gini.android.capture.internal.camera.view.education.qrcode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.internal.camera.view.education.AnimatedEducationMessageWithIntro
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun QrCodeEducationPopupContent(
    modifier: Modifier = Modifier,
    qrEducationType: QrEducationType,
    onComplete: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .fillMaxSize()
    ) {
        when (qrEducationType) {
            QrEducationType.PHOTO_DOC -> {
                AnimatedEducationMessageWithIntro(
                    modifier = modifier.align(Alignment.Center),
                    message = stringResource(R.string.gc_qr_education_photo_doc_message),
                    imagePainter = painterResource(R.drawable.gc_qr_code_education_photo_doc_image),
                    onComplete = onComplete,
                    animationKey = qrEducationType
                )
            }

            QrEducationType.UPLOAD_PICTURE -> {
                AnimatedEducationMessageWithIntro(
                    modifier = modifier.align(Alignment.Center),
                    message = stringResource(R.string.gc_qr_education_upload_picture_message),
                    imagePainter = painterResource(R.drawable.gc_qr_code_education_upload_picture_image),
                    onComplete = onComplete,
                    animationKey = qrEducationType
                )
            }
        }

        QrCodeStatusBadge(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )
    }
}

@Composable
private fun QrCodeStatusBadge(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        modifier = modifier
            .background(
                Color(context.getColor(R.color.gc_success_05)),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        text = stringResource(R.string.gc_qr_code_detected),
        color = Color(context.getColor(R.color.gc_light_01)),
        style = GiniTheme.typography.caption1
    )
}

@Composable
@Preview
private fun QrCodeEducationPopupPreview(
) {
    GiniTheme {
        QrCodeEducationPopupContent(
            qrEducationType = QrEducationType.PHOTO_DOC,
            onComplete = { /* no-op */ }
        )
    }
}
