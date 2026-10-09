package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

/**
 * Resolves the colors of the Gini palette: from the [CustomResourceProvider] when one is set and
 * answers, otherwise from the color resources. One instance belongs to one [GiniCapture]
 * instance, so a new SDK configuration starts with an empty cache.
 *
 * Must be used on the main thread.
 */
internal class GiniColorResolver(private val provider: CustomResourceProvider?) {

    private val cache = HashMap<Pair<String, Boolean>, Int?>()

    /**
     * `true` when a provider is set. When `false`, views must not be touched, so the SDK looks
     * exactly like without runtime colors.
     */
    val isActive: Boolean
        get() = provider != null

    /**
     * Returns the color for [colorRes]: the provider color when [colorRes] is a palette color and
     * the provider has a value for it, otherwise the resource color.
     */
    @ColorInt
    fun color(context: Context, @ColorRes colorRes: Int): Int {
        val name = paletteNameOf(context, colorRes) ?: return context.getColor(colorRes)
        return providerColor(name, ContextHelper.isDarkTheme(context)) ?: context.getColor(colorRes)
    }

    /**
     * Returns the color for the theme attribute [attr] when it points to a palette color, or
     * `null` when it doesn't (then the view keeps the color the theme gave it).
     */
    @ColorInt
    fun colorFromAttr(context: Context, @AttrRes attr: Int): Int? {
        val typedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.resourceId
            .takeIf { resolved && it != 0 && isPaletteColor(context, it) }
            ?.let { color(context, it) }
    }

    /**
     * Returns the Compose palette with each color taken from the provider for [isDarkMode], or
     * from [base] (by default the resource colors) when the provider has no value.
     */
    fun primitives(
        context: Context,
        isDarkMode: Boolean,
        base: GiniColorPrimitives = GiniColorPrimitives.buildColorPrimitivesBasedOnResources(context),
    ): GiniColorPrimitives {
        fun pick(name: String, default: Color): Color =
            providerColor(name, isDarkMode)?.let { Color(it) } ?: default
        return with(base) {
            GiniColorPrimitives(
                accent01 = pick("gc_accent_01", accent01),
                accent02 = pick("gc_accent_02", accent02),
                accent03 = pick("gc_accent_03", accent03),
                accent04 = pick("gc_accent_04", accent04),
                accent05 = pick("gc_accent_05", accent05),
                dark01 = pick("gc_dark_01", dark01),
                dark02 = pick("gc_dark_02", dark02),
                dark03 = pick("gc_dark_03", dark03),
                dark04 = pick("gc_dark_04", dark04),
                dark05 = pick("gc_dark_05", dark05),
                dark06 = pick("gc_dark_06", dark06),
                light01 = pick("gc_light_01", light01),
                light02 = pick("gc_light_02", light02),
                light03 = pick("gc_light_03", light03),
                light04 = pick("gc_light_04", light04),
                light05 = pick("gc_light_05", light05),
                light06 = pick("gc_light_06", light06),
                success01 = pick("gc_success_01", success01),
                success02 = pick("gc_success_02", success02),
                success03 = pick("gc_success_03", success03),
                success04 = pick("gc_success_04", success04),
                success05 = pick("gc_success_05", success05),
                error01 = pick("gc_error_01", error01),
                error02 = pick("gc_error_02", error02),
                error03 = pick("gc_error_03", error03),
                error04 = pick("gc_error_04", error04),
                error05 = pick("gc_error_05", error05),
                warning01 = pick("gc_warning_01", warning01),
                warning02 = pick("gc_warning_02", warning02),
                warning03 = pick("gc_warning_03", warning03),
                warning04 = pick("gc_warning_04", warning04),
                warning05 = pick("gc_warning_05", warning05),
            )
        }
    }

    /**
     * `true` when a provider is set and [colorRes] is one of the palette colors it is asked for.
     */
    fun isPaletteColor(context: Context, @ColorRes colorRes: Int): Boolean =
        paletteNameOf(context, colorRes) != null

    private fun paletteNameOf(context: Context, @ColorRes colorRes: Int): String? {
        if (provider == null) return null
        val resources = context.resources
        return try {
            resources.getResourceEntryName(colorRes)
                .takeIf { resources.getResourceTypeName(colorRes) == "color" && it in PALETTE_NAMES }
        } catch (ignored: Resources.NotFoundException) {
            null
        }
    }

    @ColorInt
    private fun providerColor(name: String, isDarkMode: Boolean): Int? {
        val currentProvider = provider ?: return null
        val key = name to isDarkMode
        if (!cache.containsKey(key)) cache[key] = askProvider(currentProvider, name, isDarkMode)
        return cache[key]
    }

    @ColorInt
    @Suppress("TooGenericExceptionCaught") // The provider is integrator code; any failure falls back.
    private fun askProvider(provider: CustomResourceProvider, name: String, isDarkMode: Boolean): Int? =
        try {
            provider.customPreferredColor(name, isDarkMode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.error("CustomResourceProvider failed for color $name, using the default color", e)
            null
        }

    companion object {

        private val LOG: Logger = LoggerFactory.getLogger(GiniColorResolver::class.java)

        /**
         * The resolver of the current [GiniCapture] instance, or `null` when the SDK is not
         * configured (then everything uses the color resources).
         */
        @JvmStatic
        fun current(): GiniColorResolver? =
            if (GiniCapture.hasInstance()) GiniCapture.getInstance().internal().colorResolver else null

        /**
         * The palette names a [CustomResourceProvider] is asked for.
         */
        val PALETTE_NAMES: Set<String> = setOf(
            "gc_accent_01", "gc_accent_02", "gc_accent_03", "gc_accent_04", "gc_accent_05",
            "gc_dark_01", "gc_dark_02", "gc_dark_03", "gc_dark_04", "gc_dark_05", "gc_dark_06",
            "gc_light_01", "gc_light_02", "gc_light_03", "gc_light_04", "gc_light_05", "gc_light_06",
            "gc_success_01", "gc_success_02", "gc_success_03", "gc_success_04", "gc_success_05",
            "gc_error_01", "gc_error_02", "gc_error_03", "gc_error_04", "gc_error_05",
            "gc_warning_01", "gc_warning_02", "gc_warning_03", "gc_warning_04", "gc_warning_05",
        )
    }
}
