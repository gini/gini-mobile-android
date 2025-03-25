package net.gini.android.capture.ui.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.colors.GiniColorScheme.Toggles.Thumb
import net.gini.android.capture.ui.theme.colors.GiniColorScheme.Toggles.Track

/**
 * Color scheme of Gini.
 * All colors are [Color.Unspecified] by default
 * Represents a Figma color variables and structure.
 */
@Immutable
data class GiniColorScheme(
    val background: Background = Background(),
    val bottomBar: BottomBar = BottomBar(),
    val topAppBar: TopAppBar = TopAppBar(),
    val placeholder: Placeholder = Placeholder(),
    val text: Text = Text(),
    val card: Card = Card(),
    val badge: Badge = Badge(),
    val button: Button = Button(),
    val buttonOutlined: ButtonOutlined = ButtonOutlined(),
    val textField: TextField = TextField(),
    val toggles: Toggles = Toggles(),
    val dialogs: Dialogs = Dialogs(),
    val icons: Icons = Icons(),
    val datePicker: DatePicker = DatePicker(),
    val checkbox: Checkbox = Checkbox(),
    val contextMenu: ContextMenu = ContextMenu(),
    val logo: Logo = Logo(),
) {

    @Immutable
    data class Background(
        val primary: Color = Color.Unspecified,
    )

    @Immutable
    data class BottomBar(
        val container: Color = Color.Unspecified,
        val border: Color = Color.Unspecified,
    )

    @Immutable
    data class TopAppBar(
        val container: Color = Color.Unspecified,
        val icon: Icon = Icon(),
    ) {
        @Immutable
        data class Icon(
            val action: Color = Color.Unspecified,
            val navigation: Color = Color.Unspecified,
        )
    }

    @Immutable
    data class Placeholder(
        val background: Color = Color.Unspecified,
        val tint: Color = Color.Unspecified,
    )

    @Immutable
    data class Text(
        val primary: Color = Color.Unspecified,
        val secondary: Color = Color.Unspecified,
        val tertiary: Color = Color.Unspecified,
        val accent: Color = Color.Unspecified,
        val success: Color = Color.Unspecified,
    )

    @Immutable
    data class Card(
        val container: Color = Color.Unspecified,
        val containerSuccess: Color = Color.Unspecified,
        val contentSuccess: Color = Color.Unspecified,
        val containerWarning: Color = Color.Unspecified,
        val contentWarning: Color = Color.Unspecified,
        val containerError: Color = Color.Unspecified,
        val contentError: Color = Color.Unspecified,
    )

    @Immutable
    data class Badge(
        val container: Color = Color.Unspecified,
        val content: Color = Color.Unspecified,
    )

    @Immutable
    data class Checkbox(
        val checkmark: Checkmark = Checkmark(),
        val box: Box = Box(),
    ) {
        @Immutable
        data class Checkmark(
            val checked: Color = Color.Unspecified,
            val unchecked: Color = Color.Unspecified,
            val disabled: Color = Color.Unspecified,
        )

        @Immutable
        data class Box(
            val checked: Color = Color.Unspecified,
            val unchecked: Color = Color.Unspecified,
            val disabled: Color = Color.Unspecified,
        )
    }

    @Immutable
    data class Button(
        val container: Color = Color.Unspecified,
        val containerLoading: Color = Color.Unspecified,
        val content: Color = Color.Unspecified,
    )

    @Immutable
    data class ButtonOutlined(
        val container: Color = Color.Unspecified,
        val content: Color = Color.Unspecified,
    )

    @Immutable
    data class TextField(
        val container: Color = Color.Unspecified,
        val text: Text = Text(),
        val label: Label = Label(),
        val indicator: Indicator = Indicator(),
        val cursor: Cursor = Cursor(),
        val content: Content = Content()
    ) {
        @Immutable
        data class Text(
            val focused: Color = Color.Unspecified,
            val unfocused: Color = Color.Unspecified,
            val disabled: Color = Color.Unspecified,
            val error: Color = Color.Unspecified,
        )

        @Immutable
        data class Label(
            val focused: Color = Color.Unspecified,
            val unfocused: Color = Color.Unspecified,
            val disabled: Color = Color.Unspecified,
            val error: Color = Color.Unspecified,
        )

        @Immutable
        data class Indicator(
            val focused: Color = Color.Unspecified,
            val unfocused: Color = Color.Unspecified,
            val disabled: Color = Color.Unspecified,
            val error: Color = Color.Unspecified,
        )

        @Immutable
        data class Cursor(
            val enabled: Color = Color.Unspecified,
            val error: Color = Color.Unspecified,
        )

        @Immutable
        data class Content(
            val trailing: Color = Color.Unspecified,
        )
    }

    @Immutable
    data class Toggles(
        val thumb: Thumb = Thumb(),
        val track: Track = Track()
    ) {
        @Immutable
        data class Thumb(
            val selected: Color = Color.Unspecified,
            val unselected: Color = Color.Unspecified,
        )

        @Immutable
        data class Track(
            val selected: Color = Color.Unspecified,
            val unselected: Color = Color.Unspecified,
        )
    }

    @Immutable
    data class Dialogs(
        val container: Color = Color.Unspecified,
        val text: Color = Color.Unspecified,
        val labelText: Color = Color.Unspecified
    )

    @Immutable
    data class Icons(
        val secondary: Color = Color.Unspecified,
    )

    @Immutable
    data class ContextMenu(
        val container: Color = Color.Unspecified,
        val borderColor: Color = Color.Unspecified,
    )

    @Immutable
    data class DatePicker(
        val container: Color = Color.Unspecified,
        val divider: Color = Color.Unspecified,
        val icon: Color = Color.Unspecified,

        val text: Text = Text(),
        val date: Date = Date(),
    ) {
        @Immutable
        data class Text(
            val primary: Color = Color.Unspecified,
            val secondary: Color = Color.Unspecified,
            val accent: Color = Color.Unspecified,
        )

        @Immutable
        data class Date(
            val containerFocused: Color = Color.Unspecified,
            val containerOutlined: Color = Color.Unspecified,
            val contentFocused: Color = Color.Unspecified,
            val contentOutlined: Color = Color.Unspecified,
        )
    }

    @Immutable
    data class Logo(
        val tint: Color = Color.Unspecified,
    )
}

/**
 * Created a light color scheme based on primitives.
 */
internal fun giniLightColorScheme(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = with(giniColorPrimitives) {
    GiniColorScheme(
        background = GiniColorScheme.Background(primary = light02),
        bottomBar = GiniColorScheme.BottomBar(
            container = light01,
            border = light03
        ),
        topAppBar = GiniColorScheme.TopAppBar(
            container = light01,
            icon = GiniColorScheme.TopAppBar.Icon(
                action = dark01,
                navigation = dark01
            )
        ),
        placeholder = GiniColorScheme.Placeholder(
            background = light02,
            tint = dark06
        ),
        text = GiniColorScheme.Text(
            primary = dark02,
            secondary = dark06,
            tertiary = light06,
            accent = accent01,
            success = success01
        ),
        card = GiniColorScheme.Card(
            container = light01,
            containerSuccess = success04,
            contentSuccess = success01,
            containerWarning = warning04,
            contentWarning = warning05,
            containerError = error04,
            contentError = error01
        ),
        badge = GiniColorScheme.Badge(
            container = success01,
            content = light01
        ),
        button = GiniColorScheme.Button(
            container = accent01,
            containerLoading = accent01.copy(alpha = 0.24f),
            content = light01
        ),
        buttonOutlined = GiniColorScheme.ButtonOutlined(
            container = light04,
            content = dark02
        ),
        textField = GiniColorScheme.TextField(
            container = light01,
            text = GiniColorScheme.TextField.Text(
                focused = dark02,
                unfocused = dark02,
                disabled = dark02,
                error = error02
            ), label = GiniColorScheme.TextField.Label(
                focused = accent01,
                unfocused = dark06,
                disabled = dark06,
                error = error02
            ), indicator = GiniColorScheme.TextField.Indicator(
                focused = accent01,
                unfocused = light03,
                disabled = light01,
                error = error02
            ), cursor = GiniColorScheme.TextField.Cursor(
                enabled = accent01,
                error = error02
            ), content = GiniColorScheme.TextField.Content(
                trailing = dark06
            )
        ),
        toggles = GiniColorScheme.Toggles(
            thumb = Thumb(
                selected = light01,
                unselected = light01
            ), track = Track(
                selected = accent01,
                unselected = light04
            )
        ),
        dialogs = GiniColorScheme.Dialogs(
            container = light03,
            text = dark01,
            labelText = accent01
        ),
        icons = GiniColorScheme.Icons(secondary = light06),
        datePicker = GiniColorScheme.DatePicker(
            container = light01,
            divider = light03,
            icon = dark01,
            text = GiniColorScheme.DatePicker.Text(
                primary = dark02,
                secondary = dark06,
                accent = accent01
            ),
            date = GiniColorScheme.DatePicker.Date(
                containerFocused = accent01,
                containerOutlined = accent01,
                contentFocused = light01,
                contentOutlined = accent01
            )
        ),
        checkbox = GiniColorScheme.Checkbox(
            checkmark = GiniColorScheme.Checkbox.Checkmark(
                checked = light01,
                unchecked = light01,
                disabled = light04
            ),
            box = GiniColorScheme.Checkbox.Box(
                checked = accent01,
                unchecked = dark03,
                disabled = dark06
            )
        ),
        contextMenu = GiniColorScheme.ContextMenu(
            container = light01,
            borderColor = light03
        ),
        logo = GiniColorScheme.Logo(
            tint = accent01
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
        background = GiniColorScheme.Background(primary = dark01),
        bottomBar = GiniColorScheme.BottomBar(
            container = dark02,
            border = dark03
        ),
        topAppBar = GiniColorScheme.TopAppBar(
            container = dark02,
            icon = GiniColorScheme.TopAppBar.Icon(
                action = light01,
                navigation = light01
            )
        ),
        placeholder = GiniColorScheme.Placeholder(
            background = dark04,
            tint = light06
        ),
        text = GiniColorScheme.Text(
            primary = light01,
            secondary = light06,
            tertiary = dark06,
            accent = accent01,
            success = success01
        ),
        card = GiniColorScheme.Card(
            container = dark02,
            containerSuccess = success04,
            contentSuccess = success01,
            containerWarning = warning04,
            contentWarning = warning05,
            containerError = error04,
            contentError = error01
        ),
        badge = GiniColorScheme.Badge(
            container = success01,
            content = light01
        ),
        button = GiniColorScheme.Button(
            container = accent01,
            containerLoading = accent01.copy(alpha = 0.24f),
            content = light01
        ),
        buttonOutlined = GiniColorScheme.ButtonOutlined(
            container = dark04,
            content = light01
        ),
        textField = GiniColorScheme.TextField(
            container = dark02,
            text = GiniColorScheme.TextField.Text(
                focused = light01,
                unfocused = light01,
                disabled = light01,
                error = error02
            ),
            label = GiniColorScheme.TextField.Label(
                focused = accent01,
                unfocused = light06,
                disabled = light06,
                error = error02
            ),
            indicator = GiniColorScheme.TextField.Indicator(
                focused = accent01,
                unfocused = dark03,
                disabled = dark02,
                error = error02
            ),
            cursor = GiniColorScheme.TextField.Cursor(
                enabled = accent01,
                error = error02
            ),
            content = GiniColorScheme.TextField.Content(
                trailing = light06
            )
        ),
        toggles = GiniColorScheme.Toggles(
            thumb = Thumb(
                selected = light01,
                unselected = light01
            ), track = Track(
                selected = accent01,
                unselected = dark04
            )
        ),
        dialogs = GiniColorScheme.Dialogs(
            container = dark03,
            text = light01,
            labelText = accent01
        ),
        icons = GiniColorScheme.Icons(secondary = dark06),
        datePicker = GiniColorScheme.DatePicker(
            container = dark03,
            divider = dark04,
            icon = light01,
            text = GiniColorScheme.DatePicker.Text(
                primary = light01,
                secondary = light06,
                accent = accent01
            ),
            date = GiniColorScheme.DatePicker.Date(
                containerFocused = accent01,
                containerOutlined = accent01,
                contentFocused = light01,
                contentOutlined = accent01
            )
        ),
        checkbox = GiniColorScheme.Checkbox(
            checkmark = GiniColorScheme.Checkbox.Checkmark(
                checked = light01,
                unchecked = light01,
                disabled = light04
            ),
            box = GiniColorScheme.Checkbox.Box(
                checked = accent01,
                unchecked = light06,
                disabled = dark06
            )
        ),
        contextMenu = GiniColorScheme.ContextMenu(
            container = dark02,
            borderColor = dark03
        ),
        logo = GiniColorScheme.Logo(
            tint = accent01
        )
    )
}

