package net.gini.android.capture.ui.theme.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import net.gini.android.capture.R


@Immutable
data class GiniTypography(
    val headline1: TextStyle = TextStyle.Default,
    val headline2: TextStyle = TextStyle.Default,
    val headline3: TextStyle = TextStyle.Default,
    val headline4: TextStyle = TextStyle.Default,
    val headline5: TextStyle = TextStyle.Default,
    val headline6: TextStyle = TextStyle.Default,
    val body1: TextStyle = TextStyle.Default,
    val body2: TextStyle = TextStyle.Default,
    val subtitle1: TextStyle = TextStyle.Default,
    val subtitle2: TextStyle = TextStyle.Default,
    val button: TextStyle = TextStyle.Default,
    val caption1: TextStyle = TextStyle.Default,
    val overline: TextStyle = TextStyle.Default,
)

fun TextStyle.bold() = this.copy(fontWeight = FontWeight.Bold)

@Composable
fun extractGiniTypography() = GiniTypography(
    // https://material.io/blog/migrating-material-3
    headline1 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline1),
    headline2 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline2),
    headline3 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline3),
    headline4 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline4),
    headline5 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline5),
    headline6 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Headline6),
    body1 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Body1),
    body2 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Body2),
    subtitle1 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Subtitle1),
    subtitle2 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Subtitle2),
    button = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Button),
    caption1 = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Caption1),
    overline = textStyleFromTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Overline),
)
