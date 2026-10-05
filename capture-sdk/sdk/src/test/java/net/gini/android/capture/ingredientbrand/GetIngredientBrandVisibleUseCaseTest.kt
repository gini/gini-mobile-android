package net.gini.android.capture.ingredientbrand

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.junit.Test

class GetIngredientBrandVisibleUseCaseTest {

    @Test
    fun `returns false by default`() {
        val useCase = GetIngredientBrandVisibleUseCase(GiniBankConfigurationProvider())

        assertThat(useCase(IngredientBrandScreen.ANALYSIS)).isFalse()
    }

    @Test
    fun `returns false when the configuration lists no screens`() {
        val useCase = useCaseFor(emptySet())

        assertThat(useCase(IngredientBrandScreen.ANALYSIS)).isFalse()
    }

    @Test
    fun `returns true when the configuration lists the screen`() {
        val useCase = useCaseFor(setOf("Analysis"))

        assertThat(useCase(IngredientBrandScreen.ANALYSIS)).isTrue()
    }

    @Test
    fun `matches the screen name regardless of its case`() {
        listOf("analysis", "ANALYSIS", "aNaLySiS").forEach { name ->
            assertThat(useCaseFor(setOf(name))(IngredientBrandScreen.ANALYSIS)).isTrue()
        }
    }

    /**
     * A screen name the SDK does not know must never enable the brand element somewhere else.
     */
    @Test
    fun `ignores screen names the SDK does not know`() {
        val useCase = useCaseFor(setOf("Camera", "Review", ""))

        assertThat(useCase(IngredientBrandScreen.ANALYSIS)).isFalse()
    }

    @Test
    fun `returns true when a known screen is listed next to unknown ones`() {
        val useCase = useCaseFor(setOf("Camera", "Analysis", "Review"))

        assertThat(useCase(IngredientBrandScreen.ANALYSIS)).isTrue()
    }

    private fun useCaseFor(screens: Set<String>) = GetIngredientBrandVisibleUseCase(
        GiniBankConfigurationProvider().apply {
            update { it.copy(ingredientBrandScreens = screens) }
        }
    )
}
