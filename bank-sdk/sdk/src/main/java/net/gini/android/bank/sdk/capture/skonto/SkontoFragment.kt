@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.bank.sdk.capture.skonto

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.colors.SkontoScreenColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInvoiceScanSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.WithoutSkontoSectionColors
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialog
import net.gini.android.capture.ui.components.switcher.GiniSwitch
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class SkontoFragment : Fragment() {

    val bottomNavBar = GiniCapture.getInstance().internal().navigationBarTopAdapterInstance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val viewModel = ViewModelProvider(requireActivity())[SkontoFragmentViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    ScreenContent(
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    viewModel: SkontoFragmentViewModel,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()
    ScreenStateContent(
        modifier = modifier,
        state = state,
        screenColorScheme = screenColorScheme,
        onDiscountSectionActiveChange = viewModel::onSkontoActiveChanged,
        onSkontoAmountChange = viewModel::onSkontoAmountFieldChanged,
        onDueDateChanged = viewModel::onSkontoDueDateChanged,
        onFullAmountChange = viewModel::onFullAmountFieldChanged
    )
}

@Composable
private fun ScreenStateContent(
    state: SkontoFragmentContract.State,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (String) -> Unit,
    onFullAmountChange: (String) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors()
) {
    when (state) {
        SkontoFragmentContract.State.Idle -> TODO()
        is SkontoFragmentContract.State.Ready -> ScreenReadyState(
            modifier = modifier,
            state = state,
            screenColorScheme = screenColorScheme,
            onDiscountSectionActiveChange = onDiscountSectionActiveChange,
            onDiscountAmountChange = onSkontoAmountChange,
            onDueDateChanged = onDueDateChanged,
            onFullAmountChange = onFullAmountChange
        )
    }

}

@Composable
private fun ScreenReadyState(
    state: SkontoFragmentContract.State.Ready,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onDiscountAmountChange: (String) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onFullAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
) {
    val scrollState = rememberScrollState()
    Scaffold(modifier = modifier,
        containerColor = screenColorScheme.backgroundColor,
        topBar = { TopAppBar(colors = screenColorScheme.topAppBarColors) },
        bottomBar = {
            FooterSection(
                colors = screenColorScheme.footerSectionColors,
                discountValue = state.discountValue,
                totalAmount = state.totalAmount,
                currency = state.currency
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            YourInvoiceScanSection(
                modifier = Modifier.padding(vertical = 8.dp),
                colorScheme = screenColorScheme.invoiceScanSectionColors,
            )
            SkontoSection(
                modifier = Modifier.padding(vertical = 16.dp),
                colors = screenColorScheme.skontoSectionColors,
                amount = state.skontoAmount,
                dueDate = state.discountDueDate,
                infoPaymentInDays = state.paymentInDays,
                infoDiscountValue = state.discountValue,
                onActiveChange = onDiscountSectionActiveChange,
                isActive = state.isSkontoSectionActive,
                currencyCode = "EUR",
                onSkontoAmountChange = onDiscountAmountChange,
                onDueDateChanged = onDueDateChanged,
            )
            WithoutSkontoSection(
                colors = screenColorScheme.withoutSkontoSectionColors,
                isActive = !state.isSkontoSectionActive,
                amount = state.fullAmount,
                currencyCode = "EUR",
                onFullAmountChange = onFullAmountChange
            )
        }
    }
}

@Composable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    colors: GiniTopBarColors,
) {
    GiniTopBar(
        modifier = modifier,
        colors = colors,
        title = stringResource(id = R.string.gbs_skonto_screen_title),
        navigationIcon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.ArrowBack),
                contentDescription = null,
            )
        },
        actions = {
            Icon(
                modifier = Modifier.padding(16.dp),
                painter = painterResource(net.gini.android.capture.R.drawable.gc_help_icon),
                contentDescription = null,
            )
        })
}

@Composable
private fun YourInvoiceScanSection(
    modifier: Modifier = Modifier,
    colorScheme: SkontoInvoiceScanSectionColors,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colorScheme.cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .background(colorScheme.iconBackgroundColor, shape = RoundedCornerShape(4.dp))
            ) {
                Icon(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    painter = painterResource(id = R.drawable.gbs_icon_document),
                    contentDescription = null,
                    tint = colorScheme.iconTint,
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(0.1f)
            ) {
                Text(
                    text = stringResource(id = R.string.gbs_skonto_section_invoice_scan_title),
                    style = GiniTheme.typography.subtitle1,
                    color = colorScheme.titleTextColor
                )
                Text(
                    text = stringResource(id = R.string.gbs_skonto_section_invoice_scan_subtitle),
                    style = GiniTheme.typography.body2,
                    color = colorScheme.subtitleTextColor
                )
            }

            Icon(
                painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.KeyboardArrowRight),
                contentDescription = null,
                tint = colorScheme.arrowTint.copy(alpha = 0.3f)
            )
        }

    }
}

@Composable
private fun SkontoSection(
    colors: SkontoSectionColors,
    amount: Float,
    currencyCode: String,
    dueDate: LocalDate,
    infoPaymentInDays: Int,
    infoDiscountValue: Float,
    onActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (String) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    val amountText = if (isActive) {
        "%.2f".format(amount)
    } else {
        "$amount $currencyCode"
    }


    var isDatePickerVisible by remember { mutableStateOf(false) }
    val amountFieldFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.gbs_skonto_section_discount_title),
                    style = GiniTheme.typography.subtitle1,
                    color = colors.titleTextColor,
                )
                Box(modifier = modifier.weight(0.1f)) {
                    androidx.compose.animation.AnimatedVisibility(visible = isActive) {
                        Text(
                            text = stringResource(id = R.string.gbs_skonto_section_discount_hint_label_enabled),
                            style = GiniTheme.typography.subtitle2,
                            color = colors.enabledHintTextColor,
                        )
                    }
                }
                GiniSwitch(
                    checked = isActive,
                    onCheckedChange = onActiveChange,
                )
            }
            InfoBanner(
                paymentIn = infoPaymentInDays.toString(),
                discountValue = infoDiscountValue.toString(),
                modifier = Modifier.fillMaxWidth(),
                colors = colors.infoBannerColors,
            )

            GiniTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = isActive,
                colors = colors.amountFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                onValueChange = onSkontoAmountChange,
                text = amountText,
                label = stringResource(id = R.string.gbs_skonto_section_discount_field_amount_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Text(
                            text = currencyCode,
                            style = GiniTheme.typography.subtitle1,
                        )
                    }
                },
            )

            GiniTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .onFocusChanged {
                        if (it.isFocused) {
                            isDatePickerVisible = true
                            focusManager.clearFocus()
                        }
                    }
                    .focusRequester(amountFieldFocusRequester),
                enabled = isActive,
                colors = colors.dueDateTextFieldColor,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                onValueChange = { /* Ignored */ },
                text = dueDate.format(dateFormatter),
                label = stringResource(id = R.string.gbs_skonto_section_discount_field_due_date_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Icon(
                            painter = painterResource(id = R.drawable.gbs_icon_calendar),
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }

    if (isDatePickerVisible) {
        GiniDatePickerDialog(onDismissRequest = { isDatePickerVisible = false }, onSaved = {
            isDatePickerVisible = false
            onDueDateChanged(it)
        })
    }
}

@Composable
private fun InfoBanner(
    colors: SkontoSectionColors.InfoBannerColors,
    paymentIn: String,
    discountValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.background(
            color = colors.backgroundColor, RoundedCornerShape(8.dp)
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(8.dp),
            painter = painterResource(id = R.drawable.gbs_icon_important_info),
            contentDescription = null,
            tint = colors.iconTint,
        )
        Text(
            text = stringResource(
                id = R.string.gbs_skonto_section_discount_info_banner_message,
                paymentIn,
                discountValue
            ),
            style = GiniTheme.typography.subtitle2,
            color = colors.textColor,
        )
    }
}

@Composable
private fun WithoutSkontoSection(
    colors: WithoutSkontoSectionColors,
    amount: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
    onFullAmountChange: (String) -> Unit,
    isActive: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.gbs_skonto_section_without_discount_title),
                    style = GiniTheme.typography.subtitle1,
                    color = colors.titleTextColor,
                )
                AnimatedVisibility(visible = isActive) {
                    Text(
                        text = stringResource(id = R.string.gbs_skonto_section_discount_hint_label_enabled),
                        modifier = Modifier.weight(0.1f),
                        style = GiniTheme.typography.subtitle2,
                        color = colors.enabledHintTextColor,
                    )
                }
            }
            GiniTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = isActive,
                colors = colors.amountFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                onValueChange = { onFullAmountChange(it) },
                text = amount,
                label = stringResource(id = R.string.gbs_skonto_section_without_discount_field_amount_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Text(
                            text = currencyCode,
                            style = GiniTheme.typography.subtitle1,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FooterSection(
    totalAmount: Float,
    discountValue: Float,
    currency: String,
    colors: SkontoFooterSectionColors,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.gbs_skonto_section_footer_title),
                style = GiniTheme.typography.body1,
                color = colors.titleTextColor,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val amount by animateFloatAsState(targetValue = totalAmount, label = "")
                Text(
                    text = "${"%.2f".format(amount)} $currency",
                    style = GiniTheme.typography.headline5,
                    color = colors.amountTextColor,
                )
                Box(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 12.dp)
                        .background(
                            colors.discountLabelColorScheme.backgroundColor,
                            RoundedCornerShape(4.dp)
                        ),
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        text = stringResource(
                            id = R.string.gbs_skonto_section_footer_label_discount,
                            "$discountValue%"
                        ),
                        style = GiniTheme.typography.caption1,
                        color = colors.discountLabelColorScheme.textColor,
                    )
                }
            }
            GiniButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.gbs_skonto_section_footer_continue_button_text),
                onClick = { /*TODO*/ },
                giniButtonColors = colors.continueButtonColors
            )
        }
    }
}

@Composable
@Preview
private fun ScreenReadyStatePreviewLight() {
    ScreenReadyStatePreview()
}

@Composable
@Preview(uiMode = UI_MODE_NIGHT_YES)
private fun ScreenReadyStatePreviewDark() {
    ScreenReadyStatePreview()
}

@Composable
private fun ScreenReadyStatePreview() {
    GiniTheme {
        var state by remember { mutableStateOf(previewState) }

        ScreenReadyState(
            state = state,
            onDiscountSectionActiveChange = {
                state = state.copy(isSkontoSectionActive = !state.isSkontoSectionActive)
            },
            onDiscountAmountChange = {},
            onDueDateChanged = {},
            onFullAmountChange = {},
        )
    }
}

private val previewState = SkontoFragmentContract.State.Ready(
    isSkontoSectionActive = true,
    paymentInDays = 14,
    discountValue = 3.0f,
    skontoAmount = 97.0f,
    discountDueDate = LocalDate.now(),
    fullAmount = "100.00",
    totalAmount = 97.0f,
    totalDiscount = 3.0f,
    currency = "EUR"
)