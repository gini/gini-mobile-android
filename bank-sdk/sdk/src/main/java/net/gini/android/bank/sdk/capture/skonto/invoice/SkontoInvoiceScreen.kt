package net.gini.android.bank.sdk.capture.skonto.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.invoice.colors.SkontoInvoicePreviewScreenColors
import net.gini.android.bank.sdk.capture.skonto.invoice.colors.section.SkontoInvoicePreviewScreenFooterColors
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SkontoInvoiceScreen(
    navigateBack: () -> Unit,
    viewModel: SkontoInvoiceFragmentViewModel,
    modifier: Modifier = Modifier,
    amountFormatter: AmountFormatter = getGiniBankKoin().get(),
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()

    SkontoInvoiceScreenContent(
        modifier = modifier,
        state = state,
        onCloseClicked = navigateBack,
        colors = colors,
        amountFormatter = amountFormatter,
    )
}

private const val DEFAULT_ZOOM = 1f

@Composable
private fun SkontoInvoiceScreenContent(
    amountFormatter: AmountFormatter,
    state: SkontoInvoiceFragmentState,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors(),
) {


    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddings ->
        Box(
            modifier = modifier
                .padding(paddings)
                .fillMaxSize()
                .background(colors.background)
        ) {

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center), visible = state.isLoading
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 64.dp),
                visible = !state.isLoading
            ) {
                ImagesList(
                    modifier = Modifier,
                    pages = state.images
                )
            }

            GiniTopBar(
                title = stringResource(id = R.string.gbs_skonto_invoice_preview_title),
                colors = colors.topBarColors,
                navigationIcon = {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = onCloseClicked)
                            .padding(start = 16.dp, end = 32.dp),
                        painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
                        contentDescription = null,
                        tint = colors.topBarColors.navigationContentColor
                    )
                }
            )

            state.skontoData?.let { data ->
                Footer(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    expireDate = data.skontoDueDate,
                    finalAmount = data.skontoAmountToPay,
                    fullAmount = data.fullAmountToPay,
                    colors = colors.footerColors,
                    amountFormatter = amountFormatter
                )
            }
        }
    }
}


@Composable
private fun Footer(
    amountFormatter: AmountFormatter,
    expireDate: LocalDate,
    finalAmount: Amount,
    fullAmount: Amount,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenFooterColors,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Column(
        modifier = modifier
            .background(colors.backgroundColor)
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                id = R.string.gbs_skonto_invoice_preview_expire_date,
                dateFormatter.format(expireDate)
            ),
            style = GiniTheme.typography.caption1,
            color = colors.contentColor
        )
        Text(
            text = stringResource(
                id = R.string.gbs_skonto_invoice_preview_final_amount,
                amountFormatter.format(finalAmount)
            ),
            style = GiniTheme.typography.caption1,
            color = colors.contentColor
        )
        Text(
            text = stringResource(
                id = R.string.gbs_skonto_invoice_preview_full_amount,
                amountFormatter.format(fullAmount)
            ),
            style = GiniTheme.typography.caption1,
            color = colors.contentColor
        )
    }
}

@Composable
private fun ImagesList(
    pages: List<Bitmap>,
    modifier: Modifier = Modifier,
    minZoom: Float = DEFAULT_ZOOM,
    onScaleChanged: (Float) -> Unit = {},
) {
    ZoomableLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        onScaleChanged = onScaleChanged,
        minScale = minZoom,
    ) {
        items(pages) {
            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomOut() {
    GiniTheme {
        SkontoInvoiceScreenContent(
            state = SkontoInvoiceFragmentState(
                isLoading = true,
                images = emptyList(),
                skontoData = previewSkontoData,
            ),
            onCloseClicked = {},
            amountFormatter = AmountFormatter(currencyFormatterWithoutSymbol())
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomIn() {
    GiniTheme {
        SkontoInvoiceScreenContent(
            state = SkontoInvoiceFragmentState(
                isLoading = true,
                images = emptyList(),
                skontoData = previewSkontoData,
            ),
            onCloseClicked = {},
            amountFormatter = AmountFormatter(currencyFormatterWithoutSymbol())
        )
    }
}

private val previewSkontoData = SkontoData(
    skontoPercentageDiscounted = BigDecimal.TEN,
    skontoPaymentMethod = SkontoData.SkontoPaymentMethod.Unspecified,
    skontoAmountToPay = Amount(
        value = BigDecimal.TEN,
        currency = AmountCurrency.EUR
    ), fullAmountToPay = Amount(
        value = BigDecimal.TEN,
        currency = AmountCurrency.EUR
    ), skontoRemainingDays = 51,
    skontoDueDate = LocalDate.now()
)
