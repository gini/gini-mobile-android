package net.gini.android.capture.analysis.paymentDueHint.qrcode


import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.analysis.paymentDueHint.colors.PaymentDueHintColors
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun PaymentDueHintContent(
    dueDate: String,
    onDismiss: () -> Unit,
    screenColorScheme: PaymentDueHintColors = PaymentDueHintColors.colors(),
) {
    val maxCardWidth = 360.dp

    Surface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = maxCardWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TipCard(screenColorScheme, dueDate)
            Spacer(modifier = Modifier.height(4.dp))
            DismissCard(screenColorScheme, onDismiss)
        }
    }
}


@Composable
fun TipCard(
    screenColorScheme: PaymentDueHintColors,
    dueDate: String
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = screenColorScheme.tipBackgroundColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(
                width = 1.dp,
                color = screenColorScheme.tipContentWarningColor,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .size(18.dp),
                painter = painterResource(id = R.drawable.gc_alert_triangle_icon),
                contentDescription = stringResource(R.string.gc_warning_icon_content_description),
                tint = screenColorScheme.tipContentWarningColor
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.gc_due_date_hint_tip))
                    }
                    append(
                        stringResource(
                            R.string.gc_due_date_hint,
                            dueDate
                        )
                    )
                },
                color = screenColorScheme.tipContentWarningColor,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
                style = GiniTheme.typography.caption1,
            )
        }
    }
}

@Composable
fun DismissCard(
    screenColorScheme: PaymentDueHintColors,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp)
            .border(
                width = 1.dp,
                color = screenColorScheme.buttonBorderColor,
                shape = RoundedCornerShape(4.dp)
            ),
        onClick = onDismiss

    ) {
        Column(
            modifier = Modifier.padding(top = 17.dp, start = 2.dp, end = 2.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.gc_dismiss_message),
                color = screenColorScheme.buttonTextColor,
                style = GiniTheme.typography.button,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedProgressBar(screenColorScheme = screenColorScheme, onFinished = onDismiss)
        }
    }
}

@Composable
fun AnimatedProgressBar(
    durationMillis: Int = 3_000,
    onFinished: () -> Unit = {},
    screenColorScheme: PaymentDueHintColors,
) {
    // Persist when the countdown started and whether we've already notified finish
    val startedAt = rememberSaveable { SystemClock.elapsedRealtime() }
    var finished by rememberSaveable { mutableStateOf(false) }

    // Compute elapsed/remaining and initial progress after any recreation (e.g., rotation)
    val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0)
    val remaining = (durationMillis - elapsed).coerceAtLeast(0).toInt()
    val initial = (elapsed.toFloat() / durationMillis).coerceIn(0f, 1f)

    // Drive progress explicitly
    val progress = remember { Animatable(initial) }

    LaunchedEffect(remaining) {
        // Ensure we're at the correct starting fraction after recreation
        progress.snapTo(initial)

        if (!finished) {
            if (remaining == 0) {
                finished = true
                onFinished()
            } else {
                // Animate only for the time left
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remaining, easing = LinearEasing)
                )
                if (!finished) {
                    finished = true
                    onFinished()
                }
            }
        }
    }

    LinearProgressIndicator(
        progress = { progress.value },
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        color = screenColorScheme.buttonProgressBarColor,
        trackColor = screenColorScheme.buttonBorderColor
    )
}


@Composable
private fun ScreenReadyStatePreview() {
    GiniTheme {
        PaymentDueHintContent(onDismiss = { /* no-op */ }, dueDate = "12/12/2023")
    }
}


@GiniScreenPreviewUiModes
@Composable
fun DismissibleTipScreenPreview() {
    ScreenReadyStatePreview()
}
