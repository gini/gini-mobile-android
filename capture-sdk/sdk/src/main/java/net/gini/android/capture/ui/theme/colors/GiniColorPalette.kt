package net.gini.android.capture.ui.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color scheme of Gini.
 * All colors are [Color.Unspecified] by default
 * Represents a Figma color variables and structure.
 */
@Immutable
data class GiniColorScheme(
    val background: Background = Background(),
    val button: Button = Button(),
    val icons: Icons = Icons(),
    val text: Text = Text(),
    val textField: TextField = TextField(),
    val chips: Chips = Chips(),
    val toggles: Toggles = Toggles(),
    val datePicker: DatePicker = DatePicker(),
) {

    @Immutable
    data class Background(
        val background: Color = Color.Unspecified,
        val surface: Color = Color.Unspecified,
        val bar: Color = Color.Unspecified,
        val border: Color = Color.Unspecified,
        val dialogs: Color = Color.Unspecified,
    )

    @Immutable
    data class Button(
        val surfacePrEnabled: Color = Color.Unspecified,
        val textEnabled: Color = Color.Unspecified,
    )

    @Immutable
    data class Icons(
        val surfaceFilled: Color = Color.Unspecified,
        val leading: Color = Color.Unspecified,
        val trailingPrimary: Color = Color.Unspecified,
        val trailing: Color = Color.Unspecified,
    )


    @Immutable
    data class Text(
        val system: Color = Color.Unspecified,
        val primary: Color = Color.Unspecified,
        val secondary: Color = Color.Unspecified,
        val status: Color = Color.Unspecified,
    )

    @Immutable
    data class TextField(
        val containerFocused: Color = Color.Unspecified,
        val containerUnfocused: Color = Color.Unspecified,
        val containerDisabled: Color = Color.Unspecified,
        val textFocused: Color = Color.Unspecified,
        val textUnfocused: Color = Color.Unspecified,
        val textDisabled: Color = Color.Unspecified,
        val textError: Color = Color.Unspecified,
        val indicatorFocused: Color = Color.Unspecified,
        val indicatorUnfocused: Color = Color.Unspecified,
        val indicatorDisabled: Color = Color.Unspecified,
        val indicatorError: Color = Color.Unspecified,
        val trailingContentFocused: Color = Color.Unspecified,
        val trailingContentUnfocused: Color = Color.Unspecified,
        val trailingContentDisabled: Color = Color.Unspecified,
        val trailingContentError: Color = Color.Unspecified,
    )

    @Immutable
    data class Chips(
        val suggestionEnabled: Color = Color.Unspecified,
        val assistEnabled: Color = Color.Unspecified,
        val textAssistEnabled: Color = Color.Unspecified,
        val textSuggestionEnabled: Color = Color.Unspecified,
    )


    @Immutable
    data class Toggles(
        val surfaceFocused: Color = Color.Unspecified,
        val surfaceUnfocused: Color = Color.Unspecified,
        val surfaceDisabled: Color = Color.Unspecified,
        val thumbFocused: Color = Color.Unspecified,
        val thumbUnfocused: Color = Color.Unspecified,
    )

    @Immutable
    data class DatePicker(
        val dateSelected: Color = Color.Unspecified,
        val borderDate: Color = Color.Unspecified,
        val textHeadline: Color = Color.Unspecified,
        val textSupporting: Color = Color.Unspecified,
        val textButtons: Color = Color.Unspecified,
        val textDateToday: Color = Color.Unspecified,
        val textDateSelected: Color = Color.Unspecified,
        val textDateEnabled: Color = Color.Unspecified,
        val textDateDisabled: Color = Color.Unspecified,
        val textMenu: Color = Color.Unspecified,
        val divider: Color = Color.Unspecified,
        val iconMenu: Color = Color.Unspecified,
        val iconButton: Color = Color.Unspecified,
    )
}

/**
 * Created a light color scheme based on primitives.
 */
internal fun giniLightColorScheme(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = with(giniColorPrimitives) {
    GiniColorScheme(
        background = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Background(
            background = light02,
            surface = light01,
            bar = light01,
            border = light03,
            dialogs = light01
        ), button = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Button(
            surfacePrEnabled = accent01,
            textEnabled = light01,
        ), icons = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Icons(
            surfaceFilled = light02,
            leading = dark01,
            trailingPrimary = dark01,
            trailing = light06,
        ), text = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Text(
            system = accent01,
            primary = dark02,
            secondary = dark06,
            status = success01,
        ), textField = net.gini.android.capture.ui.theme.colors.GiniColorScheme.TextField(
            containerFocused = light01,
            containerUnfocused = light01,
            containerDisabled = Color.Transparent,
            textFocused = dark02,
            textUnfocused = dark02,
            textDisabled = dark06,
            textError = error02,
            indicatorFocused = light03,
            indicatorUnfocused = light01,
            indicatorDisabled = Color.Transparent,
            indicatorError = error02,
            trailingContentFocused = dark06,
            trailingContentUnfocused = dark06,
            trailingContentDisabled = dark06,
            trailingContentError = error02,
        ), chips = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Chips(
            suggestionEnabled = success01,
            assistEnabled = success04,
            textAssistEnabled = success01,
            textSuggestionEnabled = light01
        ), toggles = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Toggles(
            surfaceFocused = accent03,
            surfaceUnfocused = light03,
            surfaceDisabled = Color.White, // TODO
            thumbFocused = accent01,
            thumbUnfocused = light01,
        ), datePicker = net.gini.android.capture.ui.theme.colors.GiniColorScheme.DatePicker(
            dateSelected = accent01,
            borderDate = accent01,
            textHeadline = dark02,
            textSupporting = dark06,
            textButtons = accent01,
            textDateToday = accent01,
            textDateSelected = light01,
            textDateEnabled = dark02,
            textDateDisabled = light06,
            textMenu = dark01,
            divider = light03,
            iconMenu = dark01,
            iconButton = dark01,
        )
    )
}

/**
 * Created a dark color scheme based on primitives.
 */
internal fun giniDarkColorScheme(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = with(giniColorPrimitives) {
    GiniColorScheme(
        background = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Background(
            background = dark01,
            surface = dark02,
            bar = dark02,
            border = dark03,
            dialogs = dark03
        ), button = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Button(
            surfacePrEnabled = accent01,
            textEnabled = light01
        ), icons = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Icons(
            surfaceFilled = dark04,
            leading = light01,
            trailingPrimary = light01,
            trailing = dark06
        ), text = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Text(
            system = accent01,
            primary = light01,
            secondary = light06,
            status = success01
        ), textField = net.gini.android.capture.ui.theme.colors.GiniColorScheme.TextField(
            containerFocused = dark02,
            containerUnfocused = dark02,
            containerDisabled = Color.Transparent,
            textFocused = light01,
            textUnfocused = light01,
            textDisabled = dark06,
            textError = error02,
            indicatorFocused = dark03,
            indicatorUnfocused = dark02,
            indicatorDisabled = Color.Transparent,
            indicatorError = error02,
            trailingContentFocused = light06,
            trailingContentUnfocused = light06,
            trailingContentDisabled = light06,
            trailingContentError = error02,
        ), chips = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Chips(
            suggestionEnabled = success01,
            assistEnabled = success04,
            textAssistEnabled = success01,
            textSuggestionEnabled = light01
        ), toggles = net.gini.android.capture.ui.theme.colors.GiniColorScheme.Toggles(
            surfaceFocused = accent03,
            surfaceUnfocused = dark03,
            surfaceDisabled = Color.White, // TODO
            thumbFocused = accent01,
            thumbUnfocused = light01,
        ), datePicker = net.gini.android.capture.ui.theme.colors.GiniColorScheme.DatePicker(
            dateSelected = accent01,
            borderDate = accent01,
            textHeadline = light01,
            textSupporting = light06,
            textButtons = accent01,
            textDateToday = accent01,
            textDateSelected = light01,
            textDateEnabled = light01,
            textDateDisabled = dark06,
            textMenu = light01,
            divider = dark04,
            iconMenu = light01,
            iconButton = light01,
        )
    )
}

