package net.gini.android.capture.ui.components.radio

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders [GiniRadioButton] under Robolectric and checks its selection, click and enabled
 * behaviour, and how [GiniRadioButtonColors] maps onto the Material radio button colors.
 */
@RunWith(AndroidJUnit4::class)
class GiniRadioButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0

    @Test
    fun `a selected button is exposed as selected`() {
        composeRule.setContent { GiniTheme { GiniRadioButton(selected = true, onClick = { clicks++ }) } }

        composeRule.onNode(isSelectable()).assertIsSelected().assertIsEnabled()
    }

    @Test
    fun `a tap is reported and does not change the selection by itself`() {
        composeRule.setContent { GiniTheme { GiniRadioButton(selected = false, onClick = { clicks++ }) } }

        composeRule.onNode(isSelectable()).performClick()

        composeRule.runOnIdle { assertThat(clicks).isEqualTo(1) }
        composeRule.onNode(isSelectable()).assertIsNotSelected()
    }

    @Test
    fun `without a click listener the button is no accessibility target`() {
        composeRule.setContent { GiniTheme { GiniRadioButton(selected = true, onClick = null) } }

        composeRule.onAllNodes(isSelectable()).assertCountEquals(0)
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun `a disabled button does not report taps`() {
        composeRule.setContent {
            GiniTheme { GiniRadioButton(selected = false, onClick = { clicks++ }, enabled = false) }
        }

        composeRule.onNode(isSelectable()).assertIsNotEnabled().performClick()

        composeRule.runOnIdle { assertThat(clicks).isEqualTo(0) }
    }

    @Test
    fun `the button follows the state its host keeps`() {
        var selected by mutableStateOf(false)
        var isError by mutableStateOf(true)
        var enabled by mutableStateOf(true)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the button's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniRadioButton(
                    modifier = Modifier.padding(8.dp),
                    selected = selected,
                    onClick = { selected = true },
                    enabled = enabled,
                    isError = isError,
                    colors = GiniRadioButtonColors.colors(),
                    interactionSource = remember { MutableInteractionSource() },
                )
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNode(isSelectable()).assertIsNotSelected()

        composeRule.onNode(isSelectable()).performClick()
        isError = false
        composeRule.onNode(isSelectable()).assertIsSelected()

        enabled = false
        composeRule.onNode(isSelectable()).assertIsNotEnabled()
    }

    @Test
    fun `the Material colors follow the Gini colors`() {
        val material = materialColors(isError = false)

        assertThat(material.selectedColor).isEqualTo(COLORS.selectedColor)
        assertThat(material.unselectedColor).isEqualTo(COLORS.unselectedColor)
        assertThat(material.disabledSelectedColor).isEqualTo(COLORS.disabledColor)
        assertThat(material.disabledUnselectedColor).isEqualTo(COLORS.disabledColor)
    }

    @Test
    fun `the error color replaces the selected and unselected colors`() {
        val material = materialColors(isError = true)

        assertThat(material.selectedColor).isEqualTo(COLORS.errorColor)
        assertThat(material.unselectedColor).isEqualTo(COLORS.errorColor)
        assertThat(material.disabledSelectedColor).isEqualTo(COLORS.disabledColor)
        assertThat(material.disabledUnselectedColor).isEqualTo(COLORS.disabledColor)
    }

    @Test
    fun `the default colors come from the theme`() {
        lateinit var colors: GiniRadioButtonColors
        lateinit var expected: GiniRadioButtonColors
        lateinit var passed: GiniRadioButtonColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniRadioButtonColors.colors()
                expected = GiniRadioButtonColors(
                    selectedColor = GiniTheme.colorScheme.text.accent,
                    unselectedColor = GiniTheme.colorScheme.checkbox.box.unchecked,
                    disabledColor = GiniTheme.colorScheme.checkbox.box.disabled,
                    errorColor = GiniTheme.colorScheme.errorHint.content,
                )
                passed = GiniRadioButtonColors.colors(
                    selectedColor = COLORS.selectedColor,
                    unselectedColor = COLORS.unselectedColor,
                    disabledColor = COLORS.disabledColor,
                    errorColor = COLORS.errorColor,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(expected)
            assertThat(colors.selectedColor).isNotEqualTo(Color.Unspecified)
            assertThat(passed).isEqualTo(COLORS)
            assertThat(GiniRadioButtonColors().errorColor).isEqualTo(Color.Unspecified)
        }
    }

    private fun materialColors(isError: Boolean): RadioButtonColors {
        lateinit var material: RadioButtonColors
        composeRule.setContent {
            GiniTheme { material = COLORS.toMaterialRadioButtonColors(isError) }
        }
        return composeRule.runOnIdle { material }
    }

    private companion object {
        const val HOST_LABEL = "Host"
        val COLORS = GiniRadioButtonColors(
            selectedColor = Color.Blue,
            unselectedColor = Color.Gray,
            disabledColor = Color.LightGray,
            errorColor = Color.Red,
        )
    }
}
