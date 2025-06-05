package net.gini.android.capture.help

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertFalse

class FileImportHelpFragmentPreferenceTest {

    private lateinit var mockContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val preferenceKey = "popup_shown"

    @Before
    fun setup() {
        mockContext = mockk()
        sharedPreferences = mockk()
        editor = mockk()

        every { mockContext.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    @Test
    fun `test isIllustrationSnackBarAlreadyShown returns false when not shown`() {
        every { sharedPreferences.getBoolean(preferenceKey, false) } returns false

        val result = FileImportHelpFragmentPreferences.isIllustrationSnackBarAlreadyShown(mockContext)
        assertFalse(result)
    }

    @Test
    fun `test isIllustrationSnackBarAlreadyShown returns true when shown`() {
        every { sharedPreferences.getBoolean(preferenceKey, false) } returns true

        val result = FileImportHelpFragmentPreferences.isIllustrationSnackBarAlreadyShown(mockContext)
        assertTrue(result)
    }

    @Test
    fun `test saveIllustrationSnackBarShown saves true correctly`() {
        FileImportHelpFragmentPreferences.saveIllustrationSnackBarShown(mockContext)
        verify(exactly = 1) { editor.putBoolean(preferenceKey, true) }
        verify(exactly = 1) { editor.apply() }
    }
}