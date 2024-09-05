@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.bank.sdk.capture.skonto

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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.colors.SkontoScreenColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInfoDialogColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInvoicePreviewSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.WithoutSkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.Amount
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones
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
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SkontoFragment : Fragment() {

    private val args: SkontoFragmentArgs by navArgs<SkontoFragmentArgs>()

    private val viewModel: SkontoFragmentViewModel by getGiniBankKoin().inject {
        parametersOf(args.data)
    }

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        forcePortraitOrientationOnPhones(activity)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        customBottomNavigationBarView =
            container?.let { customBottomNavBarAdapter?.viewAdapter?.onCreateView(it) }

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
                        },
                        navigateToInvoiceScreen = {
                            findNavController()
                                .navigate(
                                    SkontoFragmentDirections.toSkontoInvoiceFragment(
                                        it,
                                        args.invoiceHighlights
                                    )
                                )
                        },
                        navigateToHelp = {
                            findNavController().navigate(SkontoFragmentDirections.toSkontoHelpFragment())
                        }
                    )
                }
            }
        }
    }
}

@Composable
@SuppressLint("ComposableNaming")
private fun SkontoFragmentViewModel.collectSideEffect(
    action: (SkontoFragmentContract.SideEffect) -> Unit
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
    navigateBack: () -> Unit,
    navigateToHelp: () -> Unit,
    viewModel: SkontoFragmentViewModel,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    navigateToInvoiceScreen: (SkontoData) -> Unit,
) {

    BackHandler { navigateBack() }

    val state by viewModel.stateFlow.collectAsState()

    viewModel.collectSideEffect {
        when (it) {
            is SkontoFragmentContract.SideEffect.OpenInvoiceScreen ->
                navigateToInvoiceScreen(it.skontoData)
        }
    }

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
        onHelpClicked = navigateToHelp,
        customBottomNavBarAdapter = customBottomNavBarAdapter,
        onProceedClicked = viewModel::onProceedClicked,
        onInfoBannerClicked = viewModel::onInfoBannerClicked,
        onInfoDialogDismissed = viewModel::onInfoDialogDismissed,
        onInvoiceClicked = viewModel::onInvoiceClicked
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
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    onInvoiceClicked: () -> Unit,
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
            onHelpClicked = onHelpClicked,
            isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
            customBottomNavBarAdapter = customBottomNavBarAdapter,
            onProceedClicked = onProceedClicked,
            onInfoBannerClicked = onInfoBannerClicked,
            onInfoDialogDismissed = onInfoDialogDismissed,
            onInvoiceClicked = onInvoiceClicked,
        )
    }

}

@Composable
private fun ScreenReadyState(
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    onInvoiceClicked: () -> Unit,
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
    discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter()
) {

    val scrollState = rememberScrollState()
    Scaffold(modifier = modifier,
        containerColor = screenColorScheme.backgroundColor,
        topBar = {
            TopAppBar(
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                colors = screenColorScheme.topAppBarColors,
                onBackClicked = onBackClicked,
                onHelpClicked = onHelpClicked
            )
        },
        bottomBar = {
            FooterSection(
                colors = screenColorScheme.footerSectionColors,
                discountValue = state.skontoPercentage,
                totalAmount = state.totalAmount,
                isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                onBackClicked = onBackClicked,
                onHelpClicked = onHelpClicked,
                customBottomNavBarAdapter = customBottomNavBarAdapter,
                onProceedClicked = onProceedClicked,
                isSkontoSectionActive = state.isSkontoSectionActive,
                savedAmount = state.savedAmount,
                discountPercentageFormatter = discountPercentageFormatter,
            )
        }) {
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
                InvoicePreviewSection(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .tabletMaxWidth(),
                    colorScheme = screenColorScheme.invoiceScanSectionColors,
                    onClick = onInvoiceClicked,
                )
                SkontoSection(
                    modifier = Modifier
                        .padding(top = 8.dp)
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
                    discountPercentageFormatter = discountPercentageFormatter,
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
                SkontoEdgeCase.PayByCashToday,
                SkontoEdgeCase.PayByCashOnly ->
                    stringResource(id = R.string.gbs_skonto_section_info_dialog_pay_cash_message)

                SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_info_dialog_date_expired_message,
                        discountPercentageFormatter.format(state.skontoPercentage.toFloat())
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
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
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
                NavigationActionBack(
                    modifier = Modifier.padding(start = 16.dp, end = 32.dp),
                    onClick = onBackClicked
                )
            }
        },
        actions = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionHelp(
                    modifier = Modifier.padding(start = 16.dp, end = 32.dp),
                    onClick = onHelpClicked
                )
            }
        })
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
            painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.ArrowBack),
            contentDescription = null,
        )
    }
}

@Composable
private fun InvoicePreviewSection(
    modifier: Modifier = Modifier,
    colorScheme: SkontoInvoicePreviewSectionColors,
    onClick: () -> Unit,
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
                painter = rememberVectorPainter(image = Icons.AutoMirrored.Default.KeyboardArrowRight),
                contentDescription = null,
                tint = colorScheme.arrowTint
            )
        }

    }
}

@Composable
private fun SkontoSection(
    colors: SkontoSectionColors,
    amount: Amount,
    dueDate: LocalDate,
    infoPaymentInDays: Int,
    infoDiscountValue: BigDecimal,
    onActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onInfoBannerClicked: () -> Unit,
    edgeCase: SkontoEdgeCase?,
    modifier: Modifier = Modifier,
    isActive: Boolean,
    discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter()
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

                Spacer(Modifier.weight(1f))

                GiniSwitch(
                    checked = isActive,
                    onCheckedChange = onActiveChange,
                )
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
                        discountPercentageFormatter.format(animatedDiscountAmount),
                        remainingDaysText
                    )

                SkontoEdgeCase.PayByCashToday ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_cash_today_message,
                        discountPercentageFormatter.format(animatedDiscountAmount)
                    )

                SkontoEdgeCase.SkontoExpired ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_date_expired_message,
                        discountPercentageFormatter.format(animatedDiscountAmount)
                    )

                SkontoEdgeCase.SkontoLastDay ->
                    stringResource(
                        id = R.string.gbs_skonto_section_discount_info_banner_pay_today_message,
                        discountPercentageFormatter.format(animatedDiscountAmount)
                    )

                else -> stringResource(
                    id = R.string.gbs_skonto_section_discount_info_banner_normal_message,
                    remainingDaysText,
                    discountPercentageFormatter.format(animatedDiscountAmount)
                )
            }

            InfoBanner(
                text = infoBannerText,
                modifier = Modifier.fillMaxWidth(),
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
private fun WithoutSkontoSection(
    colors: WithoutSkontoSectionColors,
    amount: Amount,
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
                amount = amount.value,
                currencyCode = amount.currency.name,
                onValueChange = onFullAmountChange,
                label = stringResource(id = R.string.gbs_skonto_section_without_discount_field_amount_hint),
                trailingContent = {
                    AnimatedVisibility(visible = isActive) {
                        Text(
                            text = amount.currency.name,
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
    totalAmount: Amount,
    savedAmount: Amount,
    discountValue: BigDecimal,
    colors: SkontoFooterSectionColors,
    isBottomNavigationBarEnabled: Boolean,
    isSkontoSectionActive: Boolean,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    modifier: Modifier = Modifier,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    discountPercentageFormatter: SkontoDiscountPercentageFormatter,
) {
    val animatedTotalAmount by animateFloatAsState(
        targetValue = totalAmount.value.toFloat(), label = "totalAmount"
    )
    val animatedSavedAmount by animateFloatAsState(
        targetValue = savedAmount.value.toFloat(), label = "savedAmount"
    )
    val animatedDiscountAmount by animateFloatAsState(
        targetValue = discountValue.toFloat(), label = "discountAmount"
    )
    val totalPriceText =
        "${
            currencyFormatterWithoutSymbol().format(animatedTotalAmount).trim()
        } ${totalAmount.currency.name}"

    val savedAmountText =
        stringResource(
            id = R.string.gbs_skonto_section_footer_label_save,
            "${
                currencyFormatterWithoutSymbol().format(animatedSavedAmount).trim()
            } ${savedAmount.currency.name}"
        )

    val discountLabelText = stringResource(
        id = R.string.gbs_skonto_section_footer_label_discount,
        discountPercentageFormatter.format(animatedDiscountAmount)
    )

    if (customBottomNavBarAdapter != null) {
        val ctx = LocalContext.current
        AndroidView(factory = {
            customBottomNavBarAdapter.viewAdapter.onCreateView(FrameLayout(ctx))
        }, update = {
            with(customBottomNavBarAdapter.viewAdapter) {
                setOnProceedClickListener(onProceedClicked)
                setOnBackClickListener(onBackClicked)
                setOnHelpClickListener(onHelpClicked)
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
                val buttonPaddingStart = if (isBottomNavigationBarEnabled) 16.dp else 20.dp
                val buttonPaddingEnd = if (isBottomNavigationBarEnabled) 16.dp else 20.dp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionBack(
                            modifier = Modifier.padding(start = 16.dp),
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
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionHelp(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = onHelpClicked
                        )
                    }
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
            onHelpClicked = {},
            isBottomNavigationBarEnabled = true,
            onProceedClicked = {},
            customBottomNavBarAdapter = null,
            onInfoDialogDismissed = {},
            onInfoBannerClicked = {},
            onInvoiceClicked = {}
        )
    }
}

private val previewState = SkontoFragmentContract.State.Ready(
    isSkontoSectionActive = true,
    paymentInDays = 14,
    skontoPercentage = BigDecimal("3"),
    skontoAmount = Amount.parse("97:EUR"),
    discountDueDate = LocalDate.now(),
    fullAmount = Amount.parse("100:EUR"),
    totalAmount = Amount.parse("97:EUR"),
    paymentMethod = SkontoData.SkontoPaymentMethod.PayPal,
    skontoEdgeCase = SkontoEdgeCase.PayByCashOnly,
    edgeCaseInfoDialogVisible = false,
    savedAmount = Amount.parse("3:EUR")
)