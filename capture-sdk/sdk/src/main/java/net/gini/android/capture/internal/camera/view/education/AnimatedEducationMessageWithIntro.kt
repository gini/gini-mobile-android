package net.gini.android.capture.internal.camera.view.education

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.internal.camera.view.education.colors.EducationMessageColors
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.ui.components.animation.animatedCharsLoadingIndicatorAsState
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun AnimatedEducationMessageWithIntro(
    message: String,
    introImagePainter: Painter,
    mainImagePainter: Painter,
    modifier: Modifier = Modifier,
    introductionMessageDelay: Long = 1500L,
    mainMessageDuration: Long = 3000L,
    animationKey: Any,
    onComplete: () -> Unit,
) {
    val duration = introductionMessageDelay + mainMessageDuration

    Box(modifier = modifier.fillMaxSize()) {
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
                imagePainter = introImagePainter
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
                imagePainter = mainImagePainter
            )
        }
    }
}

@Composable
internal fun EducationMessage(
    message: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier,
    colors: EducationMessageColors = EducationMessageColors.default()
) {
    val contentDescriptionMessage =
        message + "\n" + stringResource(R.string.gc_invoice_education_content_description)

    val view = LocalView.current

    // reads aloud the text when talkback is active
    LaunchedEffect(Unit) {
        view.announceForAccessibility(message)
    }

    Column(
        modifier = modifier
            .semantics {
                contentDescription = contentDescriptionMessage
            }
            .padding(horizontal = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // we don't want to show the icon in case the font size is too big to avoid
        // overlapping in small devices
        if (!isOverMaxSupportedFontScale(LocalView.current.context)) {
            Image(
                modifier = Modifier.clearAndSetSemantics { },
                painter = imagePainter,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier
                .padding(top = 16.dp)
                .clearAndSetSemantics { },
            text = message,
            style = GiniTheme.typography.body2,
            color = colors.text,
            textAlign = TextAlign.Center
        )
        val dots by animatedCharsLoadingIndicatorAsState()
        Text(
            modifier = Modifier
                .padding(top = 40.dp)
                .clearAndSetSemantics { },
            text = stringResource(R.string.gc_education_loading) + dots,
            style = GiniTheme.typography.caption1,
            color = colors.text,
            textAlign = TextAlign.Center
        )
    }
}

private fun isOverMaxSupportedFontScale(context: Context): Boolean {
    val fontScale = context.resources.configuration.fontScale
    val isLandScape = !ContextHelper.isPortraitOrientation(context)
    return if (isLandScape) fontScale > 1.0f else fontScale >= 1.7f
}

@Composable
@GiniScreenPreviewUiModes
private fun AnimatedEducationMessageWithIntroductionPreview() {
    AnimatedEducationMessageWithIntro(
        message = stringResource(R.string.gc_qr_education_photo_doc_message),
        introImagePainter = painterResource(R.drawable.gc_qr_code_education_intro_image),
        mainImagePainter = painterResource(R.drawable.gc_qr_code_education_photo_doc_image),
        onComplete = { /* no-op */ },
        animationKey = Unit,
    )
}
