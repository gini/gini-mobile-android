@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.DigitalInvoiceSkontoScreenColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoInfoDialogColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoInvoicePreviewSectionColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.bank.sdk.di.koin.giniBankViewModel
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.Amount
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialog
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.textinput.amount.GiniAmountTextInput
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth
import net.gini.android.capture.view.InjectedViewAdapterInstance
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DigitalInvoiceSkontoFragment : Fragment() {

    companion object {
        const val REQUEST_KEY = "GBS_DIGITAL_INVOICE_SKONTO_REQUEST_KEY"
        const val RESULT_KEY = "GBS_DIGITAL_INVOICE_SKONTO_RESULT_KEY"
    }

    private val args: DigitalInvoiceSkontoFragmentArgs by navArgs<DigitalInvoiceSkontoFragmentArgs>()

    private val viewModel: DigitalInvoiceSkontoViewModel by giniBankViewModel {
        parametersOf(args.data)
    }

    private val isBottomNavigationBarEnabled =
        GiniCapture.getInstance().isBottomNavigationBarEnabled

    private val customBottomNavBarAdapter:
            InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>? =
        GiniBank.digitalInvocieSkontoNavigationBarBottomAdapterInstance
    private var customBottomNavigationBarView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        ActivityHelper.forcePortraitOrientationOnPhones(activity)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        customBottomNavigationBarView =
            container?.let { customBottomNavBarAdapter?.viewAdapter?.onCreateView(it) }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GiniTheme {
                    ScreenContent(
                        viewModel = viewModel,
                        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                        customBottomNavBarAdapter = customBottomNavBarAdapter,
                        navigateBack = {
                            setFragmentResult(REQUEST_KEY, Bundle().apply {
                                putParcelable(
                                    RESULT_KEY,
                                    viewModel.provideFragmentResult()
                                )
                            }
                            )
                            findNavController().popBackStack()
                        },
                        navigateToInvoiceScreen = { documentId, infoTextLines ->
                            findNavController()
                                .navigate(
                                    DigitalInvoiceSkontoFragmentDirections.toInvoicePreviewFragment(
                                        screenTitle = context.getString(R.string.gbs_skonto_invoice_preview_title),
                                        documentId = documentId,
                                        highlightBoxes = args.data.invoiceHighlights.flatMap { it.getExistBoxes() }
                                            .toTypedArray(),
                                        infoTextLines = infoTextLines.toTypedArray()
                                    )
                                )
                        },
                        navigateToHelpScreen = {
                            findNavController().navigate(
                                DigitalInvoiceSkontoFragmentDirections.toSkontoHelpFragment()
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
@SuppressLint("ComposableNaming")
private fun DigitalInvoiceSkontoViewModel.collectSideEffect(
    action: (DigitalInvoiceSkontoSideEffect) -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sideEffectFlow.collect {
                action(it)
            }
        }
    }
}

@Composable
private fun ScreenContent(
    isBottomNavigationBarEnabled: Boolean,
    navigateBack: () -> Unit,
    navigateToHelpScreen: () -> Unit,
    navigateToInvoiceScreen: (documentId: String, infoTextLines: List<String>) -> Unit,
    viewModel: DigitalInvoiceSkontoViewModel,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: DigitalInvoiceSkontoScreenColors = DigitalInvoiceSkontoScreenColors.colors(),
) {

    BackHandler { navigateBack() }

    val state by viewModel.stateFlow.collectAsState()

    viewModel.collectSideEffect {
        when (it) {
            is DigitalInvoiceSkontoSideEffect.OpenInvoiceScreen ->
                navigateToInvoiceScreen(it.documentId, it.infoTextLines)

            DigitalInvoiceSkontoSideEffect.OpenHelpScreen ->
                navigateToHelpScreen()
        }
    }

    ScreenStateContent(
        modifier = modifier,
        state = state,
        screenColorScheme = screenColorScheme,
        onSkontoAmountChange = viewModel::onSkontoAmountFieldChanged,
        onDueDateChanged = viewModel::onSkontoDueDateChanged,
        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
        onBackClicked = navigateBack,
        onInfoBannerClicked = viewModel::onInfoBannerClicked,
        onInfoDialogDismissed = viewModel::onInfoDialogDismissed,
        onInvoiceClicked = viewModel::onInvoiceClicked,
        customBottomNavBarAdapter = customBottomNavBarAdapter,
        onHelpClicked = viewModel::onHelpClicked
    )
}

@Composable
private fun ScreenStateContent(
    state: DigitalInvoiceSkontoScreenState,
    isBottomNavigationBarEnabled: Boolean,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onBackClicked: () -> Unit,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    onInvoiceClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: DigitalInvoiceSkontoScreenColors = DigitalInvoiceSkontoScreenColors.colors()
) {
    when (state) {
        is DigitalInvoiceSkontoScreenState.Ready -> ScreenReadyState(
            modifier = modifier,
            state = state,
            screenColorScheme = screenColorScheme,
            onDiscountAmountChange = onSkontoAmountChange,
            onDueDateChanged = onDueDateChanged,
            onBackClicked = onBackClicked,
            isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
            onInfoBannerClicked = onInfoBannerClicked,
            onInfoDialogDismissed = onInfoDialogDismissed,
            onInvoiceClicked = onInvoiceClicked,
            customBottomNavBarAdapter = customBottomNavBarAdapter,
            onHelpClicked = onHelpClicked,
        )
    }

}

@Composable
private fun ScreenReadyState(
    state: DigitalInvoiceSkontoScreenState.Ready,
    onBackClicked: () -> Unit,
    onInvoiceClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    onDiscountAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: DigitalInvoiceSkontoScreenColors = DigitalInvoiceSkontoScreenColors.colors(),
) {

    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        containerColor = screenColorScheme.backgroundColor,
        bottomBar = {
            FooterSection(
                colors = screenColorScheme.footerSectionColors,
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                onBackClicked = onBackClicked,
                customBottomNavBarAdapter = customBottomNavBarAdapter,
                onHelpClicked = onHelpClicked
            )
        },
        topBar = {
            TopAppBar(
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                colors = screenColorScheme.topAppBarColors,
                onBackClicked = onBackClicked,
                onHelpClicked = onHelpClicked,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val invoicePreviewPaddingTop =
                    if (LocalContext.current.resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
                        64.dp
                    } else {
                        8.dp
                    }
                YourInvoiceScanSection(
                    modifier = Modifier
                        .padding(top = invoicePreviewPaddingTop)
                        .tabletMaxWidth(),
                    colorScheme = screenColorScheme.invoiceScanSectionColors,
                    onClick = onInvoiceClicked,
                )
                SkontoSection(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .tabletMaxWidth(),
                    colors = screenColorScheme.scontoSectionColors,
                    amount = state.skontoAmount,
                    dueDate = state.discountDueDate,
                    infoPaymentInDays = state.paymentInDays,
                    infoDiscountValue = state.skontoPercentage,
                    isActive = state.isSkontoSectionActive,
                    onSkontoAmountChange = onDiscountAmountChange,
                    onDueDateChanged = onDueDateChanged,
                    edgeCase = state.edgeCase,
                    onInfoBannerClicked = onInfoBannerClicked,
                )
            }
        }

        if (state.edgeCaseInfoDialogVisible) {
            val text = when (state.edgeCase) {
                SkontoEdgeCase.PayByCashOnly,
                SkontoEdgeCase.PayByCashToday ->
                    stringResource(id = R.string.gbs_skonto_section_info_dialog_pay_cash_message)

                SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_info_dialog_date_expired_message,
                        state.skontoPercentage.toFloat().formatAsDiscountPercentage()
                    )

                SkontoEdgeCase.SkontoLastDay ->
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
    isBottomNavigationBarEnabled: Boolean,
    colors: GiniTopBarColors,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GiniTopBar(
        modifier = modifier,
        colors = colors,
        title = stringResource(id = R.string.gbs_skonto_screen_title),
        navigationIcon = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionBack(
                    modifier = Modifier.padding(start = 16.dp, end = 32.dp),
                    onClick = onBackClicked
                )
            }
        }, actions = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionHelp(onClick = onHelpClicked)
            }
        })
}

@Composable
private fun NavigationActionBack(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier
            .width(24.dp)
            .height(24.dp),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = net.gini.android.capture.R.drawable.gc_action_bar_back),
            contentDescription = null,
        )
    }
}

@Composable
private fun NavigationActionHelp(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier
            .width(24.dp)
            .height(24.dp),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(R.drawable.gbs_help_question_icon),
            contentDescription = null,
        )
    }
}

@Composable
private fun YourInvoiceScanSection(
    colorScheme: DigitalInvoiceSkontoInvoicePreviewSectionColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colorScheme.cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier

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
                    text = stringResource(id = R.string.gbs_skonto_section_invoice_preview_title),
                    style = GiniTheme.typography.subtitle1,
                    color = colorScheme.titleTextColor
                )
                Text(
                    text = stringResource(id = R.string.gbs_skonto_invoice_section_preview_subtitle),
                    style = GiniTheme.typography.body2,
                    color = colorScheme.subtitleTextColor
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.gbs_arrow_right),
                contentDescription = null,
                tint = colorScheme.arrowTint
            )
        }

    }
}

@Composable
private fun SkontoSection(
    isActive: Boolean,
    amount: Amount,
    dueDate: LocalDate,
    infoPaymentInDays: Int,
    infoDiscountValue: BigDecimal,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onInfoBannerClicked: () -> Unit,
    edgeCase: SkontoEdgeCase?,
    colors: DigitalInvoiceSkontoSectionColors,
    modifier: Modifier = Modifier,
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
                Box {
                    androidx.compose.animation.AnimatedVisibility(visible = isActive) {
                        Text(
                            text = stringResource(id = R.string.gbs_skonto_section_discount_hint_label_enabled),
                            style = GiniTheme.typography.subtitle2,
                            color = colors.enabledHintTextColor,
                        )
                    }
                }

            }

            val animatedDiscountAmount by animateFloatAsState(
                targetValue = infoDiscountValue.toFloat(),
                label = "discountAmount"
            )

            val remainingDaysText =
                if (infoPaymentInDays != 0) {
                    pluralStringResource(
                        id = R.plurals.days,
                        count = infoPaymentInDays,
                        infoPaymentInDays.toString()
                    )
                } else {
                    stringResource(id = R.string.days_zero)
                }

            val infoBannerText = when (edgeCase) {
                SkontoEdgeCase.PayByCashOnly ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_cash_message,
                        animatedDiscountAmount.formatAsDiscountPercentage(),
                        remainingDaysText
                    )

                SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_date_expired_message,
                        animatedDiscountAmount.formatAsDiscountPercentage()
                    )

                SkontoEdgeCase.SkontoLastDay ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_today_message,
                        animatedDiscountAmount.formatAsDiscountPercentage()
                    )

                SkontoEdgeCase.PayByCashToday -> stringResource(
                    id = R.string.gbs_skonto_section_discount_info_banner_pay_cash_today_message,
                    animatedDiscountAmount.formatAsDiscountPercentage()
                )

                else -> stringResource(
                    id = R.string.gbs_skonto_section_discount_info_banner_normal_message,
                    remainingDaysText,
                    animatedDiscountAmount.formatAsDiscountPercentage()
                )
            }

            InfoBanner(
                text = infoBannerText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = when (edgeCase) {
                    SkontoEdgeCase.SkontoLastDay,
                    SkontoEdgeCase.PayByCashToday,
                    SkontoEdgeCase.PayByCashOnly -> colors.warningInfoBannerColors

                    SkontoEdgeCase.SkontoExpired -> colors.errorInfoBannerColors
                    else -> colors.successInfoBannerColors
                },
                onClicked = onInfoBannerClicked,
                clickable = edgeCase != null,
            )
            GiniAmountTextInput(
                amount = amount.value,
                currencyCode = amount.currency.name,
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
                            text = amount.currency.name,
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

@Composable
private fun FooterSection(
    isBottomNavigationBarEnabled: Boolean,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<DigitalInvoiceSkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    colors: DigitalInvoiceSkontoFooterSectionColors = DigitalInvoiceSkontoFooterSectionColors.colors(),
) {
    if (customBottomNavBarAdapter != null) {
        val ctx = LocalContext.current
        AndroidView(factory = {
            customBottomNavBarAdapter.viewAdapter.onCreateView(FrameLayout(ctx))
        }, update = {
            with(customBottomNavBarAdapter.viewAdapter) {
                setOnHelpClickListener(onHelpClicked)
                setOnBackClickListener(onBackClicked)
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
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionBack(
                            modifier = Modifier.padding(16.dp),
                            onClick = onBackClicked
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionHelp(
                            modifier = Modifier.padding(16.dp),
                            onClick = onHelpClicked
                        )
                    }
                }
            }
        }
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
    colors: DigitalInvoiceSkontoSectionColors.InfoBannerColors,
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
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
            text = text,
            style = GiniTheme.typography.subtitle2,
            color = colors.textColor,
        )
    }
}

@Composable
private fun InfoDialog(
    text: String,
    colors: DigitalInvoiceSkontoInfoDialogColors,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        properties = DialogProperties(),
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.cardBackgroundColor
            )
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
            onDiscountAmountChange = {},
            onDueDateChanged = {},
            onBackClicked = {},
            isBottomNavigationBarEnabled = true,
            onInfoDialogDismissed = {},
            onInfoBannerClicked = {},
            onInvoiceClicked = {},
            onHelpClicked = {},
            customBottomNavBarAdapter = null,
        )
    }
}

private fun Float.formatAsDiscountPercentage(): String {
    val value = BigDecimal(this.toString()).setScale(2, RoundingMode.HALF_UP)
    return "${value.toString().trimEnd('0').trimEnd('.')}%"
}

private val previewState = DigitalInvoiceSkontoScreenState.Ready(
    isSkontoSectionActive = true,
    paymentInDays = 14,
    skontoPercentage = BigDecimal("3"),
    skontoAmount = Amount.parse("97:EUR"),
    discountDueDate = LocalDate.now(),
    fullAmount = Amount.parse("100:EUR"),
    paymentMethod = SkontoData.SkontoPaymentMethod.PayPal,
    edgeCase = SkontoEdgeCase.PayByCashOnly,
    edgeCaseInfoDialogVisible = false,
)
