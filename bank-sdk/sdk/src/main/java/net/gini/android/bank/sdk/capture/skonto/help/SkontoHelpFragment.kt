package net.gini.android.bank.sdk.capture.skonto.help

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.help.colors.SkontoHelpScreenColors
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpDescriptionSectionColors
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpItemsSectionColors
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ui.components.tooltip.GiniTooltipBox
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth
import net.gini.android.capture.view.InjectedViewAdapterInstance


class SkontoHelpFragment : Fragment() {

    private val isBottomNavigationBarEnabled =
        GiniCapture.getInstance().isBottomNavigationBarEnabled

    private val customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>? =
        GiniBank.skontoHelpNavigationBarBottomAdapterInstance
    private var customBottomNavigationBarView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }

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
                        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
                        customBottomNavBarAdapter = customBottomNavBarAdapter,
                        navigateBack = {
                            findNavController()
                                .navigateUp()
                        }
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ScreenContentPreview() {
    GiniTheme {
        ScreenContent(
            navigateBack = {},
            screenColorScheme = SkontoHelpScreenColors.colors(),
            isBottomNavigationBarEnabled = false,
            customBottomNavBarAdapter = null
        )
    }
}

@Preview
@Composable
private fun ScreenContentPreviewLight() {
    GiniTheme {
        ScreenContent(
            navigateBack = {},
            screenColorScheme = SkontoHelpScreenColors.colors(),
            isBottomNavigationBarEnabled = false,
            customBottomNavBarAdapter = null
        )
    }

}

@Composable
private fun ScreenContent(
    navigateBack: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoHelpScreenColors = SkontoHelpScreenColors.colors(),
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>? = null,
) {

    BackHandler { navigateBack() }

    ScreenStateContent(
        modifier = modifier,
        screenColorScheme = screenColorScheme,
        isBottomNavigationBarEnabled = isBottomNavigationBarEnabled,
        onBackClicked = navigateBack,
        customBottomNavBarAdapter = customBottomNavBarAdapter,
    )
}

@Composable
private fun ScreenStateContent(
    onBackClicked: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>?,
    modifier: Modifier = Modifier,
    screenColorScheme: SkontoHelpScreenColors = SkontoHelpScreenColors.colors()
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
        }, bottomBar = {
            HelpCustomNavBarSection(
                isBottomNavigationBarEnabled,
                customBottomNavBarAdapter,
                onBackClicked
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
                HelpDescriptionSection(
                    modifier = Modifier
                        .tabletMaxWidth(),
                    colors = screenColorScheme.skontoHelpDescriptionSectionColors,
                )
                HelpItemsSection(
                    modifier = Modifier
                        .tabletMaxWidth(),
                    colors = screenColorScheme.skontoHelpItemsSectionColors,
                )
                HelpFooterSection(
                    modifier = Modifier
                        .tabletMaxWidth(),
                    colors = screenColorScheme.skontoHelpFooterSectionColors,
                )
            }
        }
    }

}

@Composable
private fun HelpCustomNavBarSection(
    isBottomNavigationBarEnabled: Boolean,
    customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>?,
    onBackClicked: () -> Unit,
) {
    if (customBottomNavBarAdapter != null) {
        val ctx = LocalContext.current
        AndroidView(factory = {
            customBottomNavBarAdapter.viewAdapter.onCreateView(FrameLayout(ctx))
        }, update = {
            with(customBottomNavBarAdapter.viewAdapter) {
                setOnBackClickListener(onBackClicked)
            }
        })
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            AnimatedVisibility(visible = isBottomNavigationBarEnabled) {
                NavigationActionBack(
                    modifier = Modifier.padding(16.dp),
                    onClick = onBackClicked
                )
            }

        }
    }
}

@Composable
private fun HelpFooterSection(
    colors: SkontoHelpFooterSectionColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = R.string.gbs_skonto_help_footer_text),
            style = GiniTheme.typography.body2,
            color = colors.textColor,
        )

    }
}

@Composable
private fun HelpItemsSection(
    colors: SkontoHelpItemsSectionColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HelpItem(
            modifier = Modifier.padding(16.dp),
            colors = colors,
            text = stringResource(id = R.string.gbs_skonto_help_item_capture_invoice),
            icon = painterResource(id = R.drawable.gbs_skonto_help_capture_invoice)
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp, end = 16.dp),
            color = colors.dividerColor,
            thickness = 1.dp,
        )
        HelpItem(
            modifier = Modifier.padding(16.dp),
            colors = colors,
            text = stringResource(id = R.string.gbs_skonto_help_item_identify_discount),
            icon = painterResource(id = R.drawable.gbs_skonto_help_identify_discount)
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp, end = 16.dp),
            color = colors.dividerColor,
            thickness = 1.dp,
        )
        HelpItem(
            modifier = Modifier.padding(16.dp),
            colors = colors,
            text = stringResource(id = R.string.gbs_skonto_help_item_adjust_payment_details),
            icon = painterResource(id = R.drawable.gbs_skonto_help_adjust_payment_details)
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp, end = 16.dp),
            color = colors.dividerColor,
            thickness = 1.dp,
        )
        HelpItem(
            modifier = Modifier.padding(16.dp),
            colors = colors,
            text = stringResource(id = R.string.gbs_skonto_help_item_pay_promptly),
            icon = painterResource(id = R.drawable.gbs_skonto_help_pay_promptly)
        )

    }
}


@Composable
private fun HelpItem(
    colors: SkontoHelpItemsSectionColors,
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .height(48.dp)
                .width(48.dp),
            painter = icon,
            contentDescription = null
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp),
            text = text,
            style = GiniTheme.typography.subtitle1,
            color = colors.textColor,
        )

    }
}

@Composable
private fun HelpDescriptionSection(
    colors: SkontoHelpDescriptionSectionColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(id = R.string.gbs_skonto_help_save_money_title),
            style = GiniTheme.typography.subtitle1,
            color = colors.titleTextColor,
        )
        Text(
            text = stringResource(id = R.string.gbs_skonto_help_save_money_text),
            style = GiniTheme.typography.body2,
            color = colors.descriptionTextColor,
        )
    }
}

@Composable
private fun TopAppBar(
    onBackClicked: () -> Unit,
    isBottomNavigationBarEnabled: Boolean,
    colors: GiniTopBarColors,
    modifier: Modifier = Modifier,
) {
    GiniTopBar(
        modifier = modifier,
        colors = colors,
        title = stringResource(id = R.string.gbs_skonto_help_title),
        navigationIcon = {
            AnimatedVisibility(visible = !isBottomNavigationBarEnabled) {
                NavigationActionBack(
                    modifier = Modifier.padding(start = 16.dp, end = 32.dp),
                    onClick = onBackClicked
                )
            }
        })
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
