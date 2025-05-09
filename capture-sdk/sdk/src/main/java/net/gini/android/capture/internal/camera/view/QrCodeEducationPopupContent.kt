package net.gini.android.capture.internal.camera.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun QrCodeEducationPopupContent(
    modifier: Modifier = Modifier,
    qrEducationType: QrEducationType,
    introductionMessageDelay: Long = 1500L,
    mainMessageDuration: Long = 3000L,
    onComplete: () -> Unit,
) {
    val duration = introductionMessageDelay + mainMessageDuration

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .fillMaxSize()
    ) {
        QrCodeStatusBadge(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        val value = remember {
            androidx.compose.animation.core.Animatable(
                initialValue = 0f,
                visibilityThreshold = 0f,
            )
        }

        LaunchedEffect(qrEducationType) {
            value.animateTo(duration.toFloat(), tween(duration.toInt()))
            onComplete()
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = value.value <= introductionMessageDelay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Message(
                message = stringResource(R.string.gc_qr_education_intro_message),
                imagePainter = painterResource(R.drawable.gc_qr_code_education_intro_image)
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = value.value > introductionMessageDelay && value.value < duration,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (qrEducationType) {
                QrEducationType.PHOTO_DOC -> {
                    Message(
                        modifier = Modifier.align(Alignment.Center),
                        message = stringResource(R.string.gc_qr_education_photo_doc_message),
                        imagePainter = painterResource(R.drawable.gc_qr_code_education_photo_doc_image)
                    )
                }

                QrEducationType.UPLOAD_PICTURE -> {
                    Message(
                        modifier = Modifier.align(Alignment.Center),
                        message = stringResource(R.string.gc_qr_education_upload_picture_message),
                        imagePainter = painterResource(R.drawable.gc_qr_code_education_upload_picture_image)
                    )
                }
            }

        }
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
private fun Message(
    modifier: Modifier = Modifier,
    message: String,
    imagePainter: Painter,
) {
    Column(
        modifier = modifier.padding(horizontal = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = imagePainter,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = message,
            style = GiniTheme.typography.body2,
            color = Color(LocalContext.current.getColor(R.color.gc_light_01)),
            textAlign = TextAlign.Center
        )
    }
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
