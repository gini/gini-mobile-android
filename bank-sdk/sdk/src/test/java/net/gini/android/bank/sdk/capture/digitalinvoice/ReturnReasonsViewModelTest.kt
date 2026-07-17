package net.gini.android.bank.sdk.capture.digitalinvoice

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ReturnReasonsViewModelTest {

    private lateinit var defaultLocale: Locale

    private val reasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Falsche Größe", "en" to "Wrong size")),
        GiniCaptureReturnReason("2", mapOf("de" to "Beschädigt", "en" to "Damaged"))
    )

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `resolves reason labels in the local language`() {
        // Given
        Locale.setDefault(Locale.ENGLISH)
        val viewModel = ReturnReasonsViewModel(reasonsFixture)

        // Then
        assertThat(viewModel.localizedReasons)
            .containsExactly("Wrong size", "Damaged")
            .inOrder()
    }

    @Test
    fun `falls back to german labels when the local language is not available`() {
        // Given
        Locale.setDefault(Locale.FRENCH)
        val viewModel = ReturnReasonsViewModel(reasonsFixture)

        // Then
        assertThat(viewModel.localizedReasons)
            .containsExactly("Falsche Größe", "Beschädigt")
            .inOrder()
    }

    @Test
    fun `uses an empty string when no label is available`() {
        // Given
        Locale.setDefault(Locale.FRENCH)
        val viewModel = ReturnReasonsViewModel(
            listOf(GiniCaptureReturnReason("1", mapOf("en" to "Wrong size")))
        )

        // Then
        assertThat(viewModel.localizedReasons).containsExactly("")
    }

    @Test
    fun `returns the reason at the selected position`() {
        // Given
        val viewModel = ReturnReasonsViewModel(reasonsFixture)

        // Then
        assertThat(viewModel.reasonAt(1)).isEqualTo(reasonsFixture[1])
    }
}
