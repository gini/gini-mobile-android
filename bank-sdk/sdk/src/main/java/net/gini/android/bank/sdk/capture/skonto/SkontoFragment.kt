@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.bank.sdk.capture.skonto

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.colors.SkontoScreenColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInfoDialogColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInvoiceScanSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.WithoutSkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialog
import net.gini.android.capture.ui.components.switcher.GiniSwitch
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.textinput.amount.GiniAmountTextInput
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth
import net.gini.android.capture.ui.theme.typography.bold
import net.gini.android.capture.view.InjectedViewAdapterInstance
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SkontoFragment : Fragment() {

    private val args: SkontoFragmentArgs by navArgs<SkontoFragmentArgs>()

    lateinit var cancelListener: CancelListener

    var skontoFragmentListener: SkontoFragmentListener? = null
        set(value) {
            field = value
        }

    private val isBottomNavigationBarEnabled =
        GiniCapture.getInstance().isBottomNavigationBarEnabled

    private val customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>? =
        GiniBank.skontoNavigationBarBottomAdapterInstance
    private var customBottomNavigationBarView: View? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        customBottomNavigationBarView =
            container?.let { customBottomNavBarAdapter?.viewAdapter?.onCreateView(it) }

        val viewModel = ViewModelProvider(
            factory = ViewModelFactory(args.data),
            owner = this
        )[SkontoFragmentViewModel::class.java]

        viewModel.setListener(skontoFragmentListener)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    ScreenContent(
                        viewModel = viewModel,
                        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                        customBottomNavBarAdapter = customBottomNavBarAdapter,
                        navigateBack = {
                            findNavController()
                                .navigate(SkontoFragmentDirections.toCaptureFragment())
                        }
                    )
                }
            }
        }
    }

    internal class ViewModelFactory(
        private val args: SkontoData,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SkontoFragmentViewModel(args) as T
        }
    }
}

@Composable
private fun ScreenContent(
    navigateBack: () -> Unit,
    viewModel: SkontoFragmentViewModel,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
) {

    BackHandler { navigateBack() }

    val state by viewModel.stateFlow.collectAsState()
    ScreenStateContent(
        modifier = modifier,
        state = state,
        screenColorScheme = screenColorScheme,
        onDiscountSectionActiveChange = viewModel::onSkontoActiveChanged,
        onSkontoAmountChange = viewModel::onSkontoAmountFieldChanged,
        onDueDateChanged = viewModel::onSkontoDueDateChanged,
        onFullAmountChange = viewModel::onFullAmountFieldChanged,
        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
        onBackClicked = navigateBack,
        customBottomNavBarAdapter = customBottomNavBarAdapter,
        onProceedClicked = { viewModel.onProceedClicked() },
        onInfoBannerClicked = viewModel::onInfoBannerClicked,
        onInfoDialogDismissed = viewModel::onInfoDialogDismissed
    )
}

@Composable
private fun ScreenStateContent(
    state: SkontoFragmentContract.State,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onFullAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onBackClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors()
) {
    when (state) {
        is SkontoFragmentContract.State.Ready -> ScreenReadyState(
            modifier = modifier,
            state = state,
            screenColorScheme = screenColorScheme,
            onDiscountSectionActiveChange = onDiscountSectionActiveChange,
            onDiscountAmountChange = onSkontoAmountChange,
            onDueDateChanged = onDueDateChanged,
            onFullAmountChange = onFullAmountChange,
            onBackClicked = onBackClicked,
            isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
            customBottomNavBarAdapter = customBottomNavBarAdapter,
            onProceedClicked = onProceedClicked,
            onInfoBannerClicked = onInfoBannerClicked,
            onInfoDialogDismissed = onInfoDialogDismissed,
        )
    }

}

@Composable
private fun ScreenReadyState(
    onBackClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    state: SkontoFragmentContract.State.Ready,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onDiscountAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onFullAmountChange: (BigDecimal) -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
) {

    val scrollState = rememberScrollState()
    Scaffold(modifier = modifier,
        containerColor = screenColorScheme.backgroundColor,
        topBar = {
            TopAppBar(
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                colors = screenColorScheme.topAppBarColors,
                onBackClicked = onBackClicked,
            )
        },
        bottomBar = {
            FooterSection(
                colors = screenColorScheme.footerSectionColors,
                discountValue = state.skontoPercentage,
                totalAmount = state.totalAmount,
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                onBackClicked = onBackClicked,
                customBottomNavBarAdapter = customBottomNavBarAdapter,
                onProceedClicked = onProceedClicked,
                isSkontoSectionActive = state.isSkontoSectionActive,
                savedAmount = state.savedAmount
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkontoSection(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .tabletMaxWidth(),
                    colors = screenColorScheme.skontoSectionColors,
                    amount = state.skontoAmount,
                    dueDate = state.discountDueDate,
                    infoPaymentInDays = state.paymentInDays,
                    infoDiscountValue = state.skontoPercentage,
                    onActiveChange = onDiscountSectionActiveChange,
                    isActive = state.isSkontoSectionActive,
                    onSkontoAmountChange = onDiscountAmountChange,
                    onDueDateChanged = onDueDateChanged,
                    edgeCase = state.skontoEdgeCase,
                    onInfoBannerClicked = onInfoBannerClicked,
                )
                WithoutSkontoSection(
                    modifier = Modifier.tabletMaxWidth(),
                    colors = screenColorScheme.withoutSkontoSectionColors,
                    isActive = !state.isSkontoSectionActive,
                    amount = state.fullAmount,
                    onFullAmountChange = onFullAmountChange,
                )
            }
        }

        if (state.edgeCaseInfoDialogVisible) {
            val text = when (state.skontoEdgeCase) {
                SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly ->
                    stringResource(id = R.string.gbs_skonto_section_info_dialog_pay_cash_message)

                SkontoFragmentContract.SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_info_dialog_date_expired_message,
                        state.skontoPercentage.toFloat().formatAsDiscountPercentage()
                    )

                SkontoFragmentContract.SkontoEdgeCase.SkontoLastDay ->
                    stringResource(
                        id = R.string.gbs_skonto_section_info_dialog_pay_today_message,
                    )

                null -> ""
            }
            InfoDialog(
                text = text,
                colors = screenColorScheme.infoDialogColors,
                onDismissRequest = onInfoDialogDismissed
            )
        }
    }
}

@Composable
private fun TopAppBar(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isBottomNavigationBarEnabled: Boolean,
    colors: GiniTopBarColors,
) {
    GiniTopBar(
        modifier = modifier,
        colors = colors,
        title = stringResource(id = R.string.gbs_skonto_screen_title),
        navigationIcon = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionBack(onClick = onBackClicked)
            }
        })
}

@Composable
private fun NavigationActionBack(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.ArrowBack),
            contentDescription = null,
        )
    }
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
    amount: SkontoData.Amount,
    dueDate: LocalDate,
    infoPaymentInDays: Int,
    infoDiscountValue: BigDecimal,
    onActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onInfoBannerClicked: () -> Unit,
    edgeCase: SkontoFragmentContract.SkontoEdgeCase?,
    modifier: Modifier = Modifier,
    isActive: Boolean,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    var isDatePickerVisible by remember { mutableStateOf(false) }
    Card(
        modifier = modifier,
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

            val animatedDiscountAmount by animateFloatAsState(
                targetValue = infoDiscountValue.toFloat(),
                label = "discountAmount"
            )

            val infoBannerText = when (edgeCase) {
                SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_cash_message,
                        animatedDiscountAmount.formatAsDiscountPercentage(),
                        infoPaymentInDays.toString()
                    )

                SkontoFragmentContract.SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_date_expired_message,
                        animatedDiscountAmount.formatAsDiscountPercentage()
                    )

                SkontoFragmentContract.SkontoEdgeCase.SkontoLastDay ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_today_message,
                        animatedDiscountAmount.formatAsDiscountPercentage()
                    )

                else -> stringResource(
                    id = R.string.gbs_skonto_section_discount_info_banner_normal_message,
                    infoPaymentInDays.toString(),
                    animatedDiscountAmount.formatAsDiscountPercentage()
                )
            }

            InfoBanner(
                text = infoBannerText,
                modifier = Modifier.fillMaxWidth(),
                colors = when (edgeCase) {
                    SkontoFragmentContract.SkontoEdgeCase.SkontoLastDay,
                    SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly -> colors.warningInfoBannerColors

                    SkontoFragmentContract.SkontoEdgeCase.SkontoExpired -> colors.errorInfoBannerColors
                    else -> colors.successInfoBannerColors
                },
                onClicked = onInfoBannerClicked,
                clickable = edgeCase != null,
            )
            GiniAmountTextInput(
                amount = amount.amount,
                currencyCode = amount.currencyCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = isActive,
                colors = colors.amountFieldColors,
                onValueChange = { onSkontoAmountChange(it) },
                label = stringResource(id = R.string.gbs_skonto_section_discount_field_amount_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Text(
                            text = amount.currencyCode,
                            style = GiniTheme.typography.subtitle1,
                        )
                    }
                },
            )

            val dueDateOnClickSource = remember { MutableInteractionSource() }
            val pressed by dueDateOnClickSource.collectIsPressedAsState()

            LaunchedEffect(key1 = pressed) {
                if (pressed) {
                    isDatePickerVisible = true
                }
            }

            GiniTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .focusable(false),
                enabled = isActive,
                interactionSource = dueDateOnClickSource,
                readOnly = true,
                colors = colors.dueDateTextFieldColor,
                onValueChange = { /* Ignored */ },
                text = dueDate.format(dateFormatter),
                label = stringResource(id = R.string.gbs_skonto_section_discount_field_due_date_hint),
                trailingContent = {
                    androidx.compose.animation.AnimatedVisibility(visible = isActive) {
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
        GiniDatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            onSaved = {
                isDatePickerVisible = false
                onDueDateChanged(it)
            },
            date = dueDate,
            selectableDates = getSkontoSelectableDates()
        )
    }
}

private fun getSkontoSelectableDates() = object : SelectableDates {

    val minDateCalendar = Calendar.getInstance().apply {
        set(Calendar.MILLISECONDS_IN_DAY, 0)
    }

    val maxDateCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, 6)
    }

    val minTime = minDateCalendar.timeInMillis
    val maxTime = maxDateCalendar.timeInMillis

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return (minTime..maxTime).contains(utcTimeMillis)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return (minDateCalendar.get(Calendar.YEAR)..maxDateCalendar.get(Calendar.YEAR))
            .contains(year)
    }
}

@Composable
private fun InfoBanner(
    colors: SkontoSectionColors.InfoBannerColors,
    text: String,
    clickable: Boolean,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.gbs_icon_important_info),
) {
    Row(
        modifier = modifier
            .background(
                color = colors.backgroundColor, RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClicked, enabled = clickable),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(8.dp),
            painter = icon,
            contentDescription = null,
            tint = colors.iconTint,
        )

        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = text,
            style = GiniTheme.typography.subtitle2,
            color = colors.textColor,
        )
    }
}

@Composable
private fun InfoDialog(
    text: String,
    colors: SkontoInfoDialogColors,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        properties = DialogProperties(),
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = text,
                style = GiniTheme.typography.caption1
            )
            Button(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.End),
                onClick = onDismissRequest,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.buttonTextColor,
                ),
            ) {
                Text(
                    modifier = Modifier,
                    text = stringResource(id = R.string.gbs_skonto_section_info_dialog_ok_button_text),
                    style = GiniTheme.typography.button
                )
            }
        }
    }
}

@Composable
private fun WithoutSkontoSection(
    colors: WithoutSkontoSectionColors,
    amount: SkontoData.Amount,
    modifier: Modifier = Modifier,
    onFullAmountChange: (BigDecimal) -> Unit,
    isActive: Boolean,
) {
    Card(
        modifier = modifier,
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
            GiniAmountTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = isActive,
                colors = colors.amountFieldColors,
                amount = amount.amount,
                currencyCode = amount.currencyCode,
                onValueChange = onFullAmountChange,
                label = stringResource(id = R.string.gbs_skonto_section_without_discount_field_amount_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Text(
                            text = amount.currencyCode,
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
    totalAmount: SkontoData.Amount,
    savedAmount: SkontoData.Amount,
    discountValue: BigDecimal,
    colors: SkontoFooterSectionColors,
    isBottomNavigationBarEnabled: Boolean,
    isSkontoSectionActive: Boolean,
    onBackClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    modifier: Modifier = Modifier,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
) {
    val animatedTotalAmount by animateFloatAsState(
        targetValue = totalAmount.amount.toFloat(), label = "totalAmount"
    )
    val animatedSavedAmount by animateFloatAsState(
        targetValue = savedAmount.amount.toFloat(), label = "savedAmount"
    )
    val animatedDiscountAmount by animateFloatAsState(
        targetValue = discountValue.toFloat(), label = "discountAmount"
    )
    val totalPriceText =
        "${
            currencyFormatterWithoutSymbol().format(animatedTotalAmount).trim()
        } ${totalAmount.currencyCode}"

    val savedAmountText =
        stringResource(
            id = R.string.gbs_skonto_section_footer_label_save,
            "${
                currencyFormatterWithoutSymbol().format(animatedSavedAmount).trim()
            } ${savedAmount.currencyCode}"
        )

    val discountLabelText = stringResource(
        id = R.string.gbs_skonto_section_footer_label_discount,
        animatedDiscountAmount.formatAsDiscountPercentage()
    )

    if (customBottomNavBarAdapter != null) {
        val ctx = LocalContext.current
        AndroidView(factory = {
            customBottomNavBarAdapter.viewAdapter.onCreateView(FrameLayout(ctx))
        }, update = {
            with(customBottomNavBarAdapter.viewAdapter) {
                setOnProceedClickListener(onProceedClicked)
                setOnBackClickListener(onBackClicked)
                onTotalAmountUpdated(totalPriceText)
                onSkontoPercentageBadgeUpdated(discountLabelText)
                onSkontoPercentageBadgeVisibilityUpdate(isSkontoSectionActive)
                onSkontoSavingsAmountUpdated(savedAmountText)
                onSkontoSavingsAmountVisibilityUpdated(isSkontoSectionActive)
            }
        })
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = colors.cardBackgroundColor),
        ) {
            Column(
                modifier = Modifier
                    .tabletMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                ) {
                    Row {
                        Text(
                            modifier = Modifier.weight(0.1f),
                            text = stringResource(id = R.string.gbs_skonto_section_footer_title),
                            style = GiniTheme.typography.body1,
                            color = colors.titleTextColor,
                        )
                        AnimatedVisibility(
                            visible = isSkontoSectionActive
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        colors.discountLabelColorScheme.backgroundColor,
                                        RoundedCornerShape(4.dp)
                                    ),
                            ) {
                                Text(
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                    text = discountLabelText,
                                    style = GiniTheme.typography.caption1,
                                    color = colors.discountLabelColorScheme.textColor,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = totalPriceText,
                            style = GiniTheme.typography.headline5.bold(),
                            color = colors.amountTextColor,
                        )
                    }
                    AnimatedVisibility(
                        visible = isSkontoSectionActive
                    ) {
                        Text(
                            text = savedAmountText,
                            style = GiniTheme.typography.caption1,
                            color = colors.savedAmountTextColor,
                        )
                    }
                }
                val buttonPaddingStart = if (isBottomNavigationBarEnabled) 0.dp else 20.dp
                val buttonPaddingEnd = if (isBottomNavigationBarEnabled) 48.dp else 24.dp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionBack(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            onClick = onBackClicked
                        )
                    }
                    GiniButton(
                        modifier = Modifier
                            .weight(0.1f)
                            .padding(start = buttonPaddingStart, end = buttonPaddingEnd),
                        text = stringResource(id = R.string.gbs_skonto_section_footer_continue_button_text),
                        onClick = onProceedClicked,
                        giniButtonColors = colors.continueButtonColors
                    )
                }
            }
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
            onBackClicked = {},
            isBottomNavigationBarEnabled = true,
            onProceedClicked = {},
            customBottomNavBarAdapter = null,
            onInfoDialogDismissed = {},
            onInfoBannerClicked = {},
        )
    }
}

private fun Float.formatAsDiscountPercentage(): String {
    val value = BigDecimal(this.toString()).setScale(2, RoundingMode.HALF_UP)
    return "${value.toString().trimEnd('0').trimEnd('.')}%"
}

private val previewState = SkontoFragmentContract.State.Ready(
    isSkontoSectionActive = true,
    paymentInDays = 14,
    skontoPercentage = BigDecimal("3"),
    skontoAmount = SkontoData.Amount(BigDecimal("97"), "EUR"),
    discountDueDate = LocalDate.now(),
    fullAmount = SkontoData.Amount(BigDecimal("100"), "EUR"),
    totalAmount = SkontoData.Amount(BigDecimal("97"), "EUR"),
    paymentMethod = SkontoData.SkontoPaymentMethod.PayPal,
    skontoEdgeCase = SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly,
    edgeCaseInfoDialogVisible = false,
    savedAmount = SkontoData.Amount(BigDecimal("3"), "EUR")
)