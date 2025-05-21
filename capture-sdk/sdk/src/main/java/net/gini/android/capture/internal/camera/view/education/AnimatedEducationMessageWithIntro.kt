package net.gini.android.capture.internal.camera.view.education

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.animation.animatedCharsLoadingIndicatorAsState
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun AnimatedEducationMessageWithIntro(
    message: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier,
    introductionMessageDelay: Long = 1500L,
    mainMessageDuration: Long = 3000L,
    animationKey: Any,
    onComplete: () -> Unit,
) {
    val duration = introductionMessageDelay + mainMessageDuration

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .fillMaxSize()
    ) {
        val value = remember {
            androidx.compose.animation.core.Animatable(
                initialValue = 0f,
                visibilityThreshold = 0f,
            )
        }

        LaunchedEffect(animationKey) {
            value.animateTo(duration.toFloat(), tween(duration.toInt()))
            onComplete()
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = value.value <= introductionMessageDelay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EducationMessage(
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
            EducationMessage(
                modifier = Modifier.align(Alignment.Center),
                message = message,
                imagePainter = imagePainter
            )
        }
    }
}

@Composable
internal fun EducationMessage(
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
        val dots by animatedCharsLoadingIndicatorAsState()
        Text(
            modifier = Modifier.padding(top = 40.dp),
            text = stringResource(R.string.gc_education_loading) + dots,
            style = GiniTheme.typography.caption1,
            color = Color(LocalContext.current.getColor(R.color.gc_light_01)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@GiniScreenPreviewUiModes
private fun AnimatedEducationMessageWithIntroductionPreview() {
    AnimatedEducationMessageWithIntro(
        message = stringResource(R.string.gc_qr_education_photo_doc_message),
        imagePainter = painterResource(R.drawable.gc_qr_code_education_photo_doc_image),
        onComplete = { /* no-op */ },
        animationKey = Unit,
    )
}
