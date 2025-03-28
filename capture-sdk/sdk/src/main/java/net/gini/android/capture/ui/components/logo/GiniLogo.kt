package net.gini.android.capture.ui.components.logo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniLogo(
    modifier: Modifier = Modifier,
    colors: GiniLogoColors = GiniLogoColors.colors(),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Powered by",
            style = GiniTheme.typography.body1,
            color = colors.textColor
        )
        Icon(
            painter = painterResource(R.drawable.ic_gini_logo),
            contentDescription = null,
            tint = colors.logoTint
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GiniCheckboxCheckedPreview() {
    GiniTheme {
        GiniLogo()
    }
}
