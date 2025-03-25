@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class
)

package net.gini.android.bank.sdk.capture.skonto

import android.icu.util.Calendar
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.colors.SkontoScreenColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInfoDialogColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInvoicePreviewSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.WithoutSkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.bank.sdk.capture.skonto.mapper.toErrorMessage
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoFragmentViewModel
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc.AttachDocumentToTransactionDialog
import net.gini.android.bank.sdk.util.ui.keyboardAsState
import net.gini.android.capture.Amount
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialog
import net.gini.android.capture.ui.components.switcher.GiniSwitch
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.textinput.amount.GiniAmountTextInput
import net.gini.android.capture.ui.components.tooltip.GiniTooltipBox
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth
import net.gini.android.capture.ui.theme.typography.bold
import net.gini.android.capture.util.compose.keyboardPadding
import net.gini.android.capture.view.InjectedViewAdapterInstance
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
internal fun SkontoScreenContent(
    isBottomNavigationBarEnabled: Boolean,
    navigateBack: () -> Unit,
    navigateToHelp: () -> Unit,
    amountFormatter: AmountFormatter,
    viewModel: SkontoFragmentViewModel,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    navigateToInvoiceScreen: (documentId: String, infoTextLines: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isLandScape: Boolean
) {

    BackHandler { viewModel.onBackClicked() }

    val state by viewModel.collectAsState()
    viewModel.collectSideEffect {
        when (it) {
            is SkontoScreenSideEffect.OpenInvoiceScreen ->
                navigateToInvoiceScreen(it.documentId, it.infoTextLines)

            SkontoScreenSideEffect.OpenHelpScreen -> navigateToHelp()
            SkontoScreenSideEffect.NavigateBack -> navigateBack()
        }
    }

    val keyboardState by keyboardAsState()

    LaunchedEffect(keyboardState) {
        viewModel.onKeyboardStateChanged(keyboardState)
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
        onBackClicked = viewModel::onBackClicked,
        onHelpClicked = viewModel::onHelpClicked,
        customBottomNavBarAdapter = customBottomNavBarAdapter,
        onProceedClicked = viewModel::onProceedClicked,
        onInfoBannerClicked = viewModel::onInfoBannerClicked,
        onInfoDialogDismissed = viewModel::onInfoDialogDismissed,
        onInvoiceClicked = viewModel::onInvoiceClicked,
        onConfirmAttachTransactionDocClicked = viewModel::onConfirmAttachTransactionDocClicked,
        onCancelAttachTransactionDocClicked = viewModel::onCancelAttachTransactionDocClicked,
        amountFormatter = amountFormatter,
        onSkontoAmountFieldFocused = viewModel::onSkontoAmountFieldFocused,
        onDueDateFieldFocused = viewModel::onDueDateFieldFocused,
        onFullAmountFieldFocused = viewModel::onFullAmountFieldFocused,
        isLandScape = isLandScape
    )
}

@Composable
private fun ScreenStateContent(
    state: SkontoScreenState,
    amountFormatter: AmountFormatter,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onFullAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    onInvoiceClicked: () -> Unit,
    onSkontoAmountFieldFocused: () -> Unit,
    onDueDateFieldFocused: () -> Unit,
    onFullAmountFieldFocused: () -> Unit,
    onConfirmAttachTransactionDocClicked: (alwaysAttach: Boolean) -> Unit,
    onCancelAttachTransactionDocClicked: () -> Unit,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isLandScape: Boolean
) {
    when (state) {
        is SkontoScreenState.Ready -> ScreenReadyState(
            amountFormatter = amountFormatter,
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
            onConfirmAttachTransactionDocClicked = onConfirmAttachTransactionDocClicked,
            onCancelAttachTransactionDocClicked = onCancelAttachTransactionDocClicked,
            onSkontoAmountFieldFocused = onSkontoAmountFieldFocused,
            onDueDateFieldFocused = onDueDateFieldFocused,
            onFullAmountFieldFocused = onFullAmountFieldFocused,
            isLandScape = isLandScape
        )
    }

}

@Composable
private fun ScreenReadyState(
    isBottomNavigationBarEnabled: Boolean,
    state: SkontoScreenState.Ready,
    amountFormatter: AmountFormatter,
    onConfirmAttachTransactionDocClicked: (alwaysAttach: Boolean) -> Unit,
    onCancelAttachTransactionDocClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    onInvoiceClicked: () -> Unit,
    onDiscountSectionActiveChange: (Boolean) -> Unit,
    onDiscountAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onFullAmountChange: (BigDecimal) -> Unit,
    onInfoBannerClicked: () -> Unit,
    onInfoDialogDismissed: () -> Unit,
    onSkontoAmountFieldFocused: () -> Unit,
    onDueDateFieldFocused: () -> Unit,
    onFullAmountFieldFocused: () -> Unit,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter(),
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isLandScape: Boolean
) {
    val scrollState = rememberScrollState()
    val keyboardPadding by keyboardPadding(108.dp, scrollState)

    Scaffold(
        modifier = modifier,
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
            HandleBottomBarForScreenReadyState(
                isBottomNavigationBarEnabled,
                state,
                onBackClicked,
                onHelpClicked,
                onProceedClicked,
                customBottomNavBarAdapter,
                discountPercentageFormatter,
                screenColorScheme,
                isLandScape
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(bottom = keyboardPadding),
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
                        .padding(top = getInvoicePreviewPaddingTop())
                        .tabletMaxWidth(),
                    colorScheme = screenColorScheme.invoiceScanSectionColors,
                    onClick = onInvoiceClicked,
                )
                SkontoSection(
                    amountFormatter = amountFormatter,
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
                    edgeCase = state.edgeCase,
                    onInfoBannerClicked = onInfoBannerClicked,
                    discountPercentageFormatter = discountPercentageFormatter,
                    skontoAmountValidationError = state.skontoAmountValidationError,
                    isLandScape = isLandScape,
                    onSkontoAmountFieldFocused = onSkontoAmountFieldFocused,
                    onDueDateFieldFocued = onDueDateFieldFocused,

                )
                WithoutSkontoSection(
                    modifier = Modifier.tabletMaxWidth(),
                    colors = screenColorScheme.withoutSkontoSectionColors,
                    isActive = !state.isSkontoSectionActive,
                    amount = state.fullAmount,
                    onFullAmountChange = onFullAmountChange,
                    amountFormatter = amountFormatter,
                    fullAmountValidationError = state.fullAmountValidationError,
                    onFullAmountFieldFocused = onFullAmountFieldFocused,
                )

                if (isLandScape && customBottomNavBarAdapter == null) {
                    FooterSection(
                        colors = screenColorScheme.footerSectionColors,
                        discountValue = state.skontoPercentage,
                        totalAmount = state.totalAmount,
                        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                        onBackClicked = onBackClicked,
                        onHelpClicked = onHelpClicked,
                        customBottomNavBarAdapter = null,
                        onProceedClicked = onProceedClicked,
                        isSkontoSectionActive = state.isSkontoSectionActive,
                        savedAmount = state.savedAmount,
                        discountPercentageFormatter = discountPercentageFormatter,
                        isLandScape = true
                    )
                }
            }
        }

        if (state.edgeCaseInfoDialogVisible) {
            val text = when (state.edgeCase) {
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

        if (state.transactionDialogVisible) {
            AttachDocumentToTransactionDialog(
                onDismiss = onCancelAttachTransactionDocClicked,
                onConfirm = onConfirmAttachTransactionDocClicked
            )
        }
    }
}

@Composable
private fun TopAppBar(
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    colors: GiniTopBarColors,
    isBottomNavigationBarEnabled: Boolean,
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
        },
        actions = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionHelp(
                    modifier = Modifier.padding(start = 20.dp, end = 12.dp),
                    onClick = onHelpClicked
                )
            }
        })
}

@Composable
private fun HandleBottomBarForScreenReadyState(
    isBottomNavigationBarEnabled: Boolean,
    state: SkontoScreenState.Ready,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter(),
    screenColorScheme: SkontoScreenColors = SkontoScreenColors.colors(),
    isLandScape: Boolean
) {
    if (!isLandScape || (customBottomNavBarAdapter != null)) {
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
            isLandScape = isLandScape
        )
    } else if (isBottomNavigationBarEnabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(screenColorScheme.footerSectionColors.cardBackgroundColor.value))
        ) {
            AnimatedVisibility(visible = true) {
                NavigationActionBack(
                    modifier = Modifier.padding(16.dp),
                    onClick = onBackClicked
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(visible = true) {
                NavigationActionHelp(
                    onClick = onHelpClicked,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NavigationActionHelp(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GiniTooltipBox(
        tooltipText = stringResource(
            id = R.string.gbs_skonto_screen_content_description_help
        )
    ) {
        IconButton(
            modifier = modifier
                .width(24.dp)
                .height(24.dp),
            onClick = onClick
        ) {
            Icon(
                painter = painterResource(R.drawable.gbs_help_question_icon),
                contentDescription = stringResource(
                    id = R.string.gbs_skonto_screen_content_description_help
                ),
            )
        }
    }
}

@Composable
private fun NavigationActionBack(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GiniTooltipBox(
        tooltipText = stringResource(
            id = R.string.gbs_skonto_screen_content_description_back
        )
    ) {
        IconButton(
            modifier = modifier
                .width(24.dp)
                .height(24.dp),
            onClick = onClick
        ) {
            Icon(
                painter = painterResource(id = net.gini.android.capture.R.drawable.gc_action_bar_back),
                contentDescription = stringResource(
                    id = R.string.gbs_skonto_screen_content_description_back
                ),
            )
        }
    }
}

@Composable
private fun InvoicePreviewSection(
    onClick: () -> Unit,
    colorScheme: SkontoInvoicePreviewSectionColors,
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
    amountFormatter: AmountFormatter,
    colors: SkontoSectionColors,
    amount: Amount,
    dueDate: LocalDate,
    infoPaymentInDays: Int,
    infoDiscountValue: BigDecimal,
    onActiveChange: (Boolean) -> Unit,
    onSkontoAmountChange: (BigDecimal) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onInfoBannerClicked: () -> Unit,
    onSkontoAmountFieldFocused: () -> Unit,
    onDueDateFieldFocued: () -> Unit,
    edgeCase: SkontoEdgeCase?,
    skontoAmountValidationError: SkontoScreenState.Ready.SkontoAmountValidationError?,
    modifier: Modifier = Modifier,
    discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter(),
    isLandScape: Boolean
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val resources = LocalContext.current.resources

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(0.1f, fill = false),
                        text = stringResource(id = R.string.gbs_skonto_section_discount_title),
                        style = GiniTheme.typography.subtitle1,
                        color = colors.titleTextColor,
                    )

                    AnimatedVisibility(
                        modifier = Modifier
                            .wrapContentSize()
                            .requiredWidth(IntrinsicSize.Max),
                        visible = isActive
                    ) {
                        Text(
                            text = stringResource(id = R.string.gbs_skonto_section_discount_hint_label_enabled),
                            style = GiniTheme.typography.subtitle2,
                            color = colors.enabledHintTextColor,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                }
                GiniSwitch(
                    modifier = Modifier.padding(start = 8.dp),
                    checked = isActive,
                    onCheckedChange = onActiveChange,
                )
            }
            val animatedDiscountAmount by animateFloatAsState(
                targetValue = infoDiscountValue.toFloat(),
                label = "discountAmount"
            )

            val remainingDaysText = getSkontoRemainingDays(infoPaymentInDays)

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
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
                    .padding(top = 16.dp)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onSkontoAmountFieldFocused()
                        }
                    },
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
                isError = skontoAmountValidationError != null,
                supportingText = skontoAmountValidationError?.toErrorMessage(
                    resources = resources,
                    amountFormatter = amountFormatter
                )
            )

            val dueDateOnClickSource = remember { MutableInteractionSource() }
            val pressed by dueDateOnClickSource.collectIsPressedAsState()

            LaunchedEffect(key1 = pressed) {
                if (pressed) {
                    isDatePickerVisible = true
                    onDueDateFieldFocued()
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
            isLandScape = isLandScape,
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
    isActive: Boolean,
    onFullAmountChange: (BigDecimal) -> Unit,
    onFullAmountFieldFocused: () -> Unit,
    colors: WithoutSkontoSectionColors,
    amount: Amount,
    amountFormatter: AmountFormatter,
    fullAmountValidationError: SkontoScreenState.Ready.FullAmountValidationError?,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

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
                    modifier = Modifier.weight(0.1f, fill = false),
                    text = stringResource(id = R.string.gbs_skonto_section_without_discount_title),
                    style = GiniTheme.typography.subtitle1,
                    color = colors.titleTextColor,
                )
                AnimatedVisibility(
                    modifier = Modifier
                        .wrapContentSize()
                        .requiredWidth(IntrinsicSize.Max),
                    visible = isActive
                ) {
                    Text(
                        text = stringResource(id = R.string.gbs_skonto_section_discount_hint_label_enabled),
                        style = GiniTheme.typography.subtitle2,
                        color = colors.enabledHintTextColor,
                        softWrap = false,
                        maxLines = 1,
                    )
                }
            }
            GiniAmountTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFullAmountFieldFocused()
                        }
                    },
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
                isError = fullAmountValidationError != null,
                supportingText = fullAmountValidationError?.toErrorMessage(
                    resources = resources,
                    amountFormatter = amountFormatter
                )
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
    discountPercentageFormatter: SkontoDiscountPercentageFormatter,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    isLandScape: Boolean
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
        FooterSectionWithoutCustomBottomBar(
            colors, isBottomNavigationBarEnabled, modifier, isLandScape,
            isSkontoSectionActive, discountLabelText, totalPriceText, savedAmountText,
            onBackClicked, onHelpClicked, onProceedClicked
        )
    }
}

@Composable
private fun FooterSectionWithoutCustomBottomBar(
    colors: SkontoFooterSectionColors,
    isBottomNavigationBarEnabled: Boolean,
    modifier: Modifier = Modifier,
    isLandScape: Boolean,
    isSkontoSectionActive: Boolean,
    discountLabelText: String,
    totalPriceText: String,
    savedAmountText: String,
    onBackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onProceedClicked: () -> Unit,
) {
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

            val buttonPadding = if (isBottomNavigationBarEnabled && !isLandScape)
                16.dp else 20.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!isLandScape) {
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionBack(
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = onBackClicked
                        )
                    }
                }

                GiniButton(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(start = buttonPadding, end = buttonPadding),
                    text = stringResource(id = R.string.gbs_skonto_section_footer_continue_button_text),
                    onClick = onProceedClicked,
                    giniButtonColors = colors.continueButtonColors
                )

                if (!isLandScape) {
                    AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                        NavigationActionHelp(
                            modifier = Modifier.padding(end = 20.dp),
                            onClick = onHelpClicked
                        )
                    }
                }
            }
        }
    }
}

@Composable
@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
private fun ScreenReadyStatePreviewLight() {
    ScreenReadyStatePreview()
}

@Composable
private fun ScreenReadyStatePreview(isLandScape: Boolean = false) {
    GiniTheme {
        var state by remember { mutableStateOf(previewState()) }
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
            onInvoiceClicked = {},
            onCancelAttachTransactionDocClicked = {

            },
            onConfirmAttachTransactionDocClicked = {

            },
            onDueDateFieldFocused = {},
            onSkontoAmountFieldFocused = {},
            onFullAmountFieldFocused = {},
            amountFormatter = AmountFormatter(currencyFormatterWithoutSymbol()),
            isLandScape = isLandScape
        )
    }
}

private fun previewState() = SkontoScreenState.Ready(
    isSkontoSectionActive = true,
    paymentInDays = 14,
    skontoPercentage = BigDecimal("3"),
    skontoAmount = Amount.parse("97:EUR"),
    discountDueDate = LocalDate.now(),
    fullAmount = Amount.parse("100:EUR"),
    totalAmount = Amount.parse("97:EUR"),
    paymentMethod = SkontoData.SkontoPaymentMethod.PayPal,
    edgeCase = SkontoEdgeCase.PayByCashOnly,
    edgeCaseInfoDialogVisible = false,
    savedAmount = Amount.parse("3:EUR"),
    transactionDialogVisible = false,
    skontoAmountValidationError = null,
    fullAmountValidationError = null,
)
