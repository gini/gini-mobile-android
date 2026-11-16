package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [GiniColorResolver.primitives]: every palette name lands in the matching
 * [GiniColorPrimitives] field.
 */
@RunWith(RobolectricTestRunner::class)
class GiniColorPrimitivesFromResolverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `without provider values the primitives equal the resource based primitives`() {
        val resolver = GiniColorResolver(CustomResourceProvider { _, _ -> null })

        assertThat(resolver.primitives(context, isDarkMode = false))
            .isEqualTo(GiniColorPrimitives.buildColorPrimitivesBasedOnResources(context))
    }

    @Test
    fun `every palette name maps to its own primitives field`() {
        val resolver = GiniColorResolver(CustomResourceProvider { name, _ -> colorFor(name) })

        val primitives = resolver.primitives(context, isDarkMode = false)

        FIELDS.forEach { (name, field) ->
            assertThat(field(primitives)).isEqualTo(Color(colorFor(name)))
        }
        assertThat(FIELDS.map { it.first }).containsExactlyElementsIn(GiniColorResolver.PALETTE_NAMES)
    }

    @Test
    fun `gc_warning_05 falls back to its compose default because it has no resource`() {
        val resolver = GiniColorResolver(CustomResourceProvider { _, _ -> null })

        assertThat(resolver.primitives(context, isDarkMode = false).warning05)
            .isEqualTo(Color(0xFFA17503))
    }

    @Test
    fun `the requested mode is passed to the provider`() {
        val modes = mutableSetOf<Boolean>()
        val resolver = GiniColorResolver(CustomResourceProvider { _, isDarkMode -> modes += isDarkMode; null })

        resolver.primitives(context, isDarkMode = true)

        assertThat(modes).containsExactly(true)
    }

    private fun colorFor(name: String): Int = 0xFF000000.toInt() or (name.hashCode() and 0x00FFFFFF)

    private companion object {
        val FIELDS: List<Pair<String, (GiniColorPrimitives) -> Color>> = listOf(
            "gc_accent_01" to { it.accent01 },
            "gc_accent_02" to { it.accent02 },
            "gc_accent_03" to { it.accent03 },
            "gc_accent_04" to { it.accent04 },
            "gc_accent_05" to { it.accent05 },
            "gc_dark_01" to { it.dark01 },
            "gc_dark_02" to { it.dark02 },
            "gc_dark_03" to { it.dark03 },
            "gc_dark_04" to { it.dark04 },
            "gc_dark_05" to { it.dark05 },
            "gc_dark_06" to { it.dark06 },
            "gc_light_01" to { it.light01 },
            "gc_light_02" to { it.light02 },
            "gc_light_03" to { it.light03 },
            "gc_light_04" to { it.light04 },
            "gc_light_05" to { it.light05 },
            "gc_light_06" to { it.light06 },
            "gc_success_01" to { it.success01 },
            "gc_success_02" to { it.success02 },
            "gc_success_03" to { it.success03 },
            "gc_success_04" to { it.success04 },
            "gc_success_05" to { it.success05 },
            "gc_error_01" to { it.error01 },
            "gc_error_02" to { it.error02 },
            "gc_error_03" to { it.error03 },
            "gc_error_04" to { it.error04 },
            "gc_error_05" to { it.error05 },
            "gc_warning_01" to { it.warning01 },
            "gc_warning_02" to { it.warning02 },
            "gc_warning_03" to { it.warning03 },
            "gc_warning_04" to { it.warning04 },
            "gc_warning_05" to { it.warning05 },
        )
    }
}
