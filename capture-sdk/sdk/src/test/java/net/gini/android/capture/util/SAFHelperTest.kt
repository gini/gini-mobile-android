package net.gini.android.capture.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Internal use only
 *
 * These unit tests check that the SAFHelper methods correctly handle
 * Android Storage Access Framework logic (permissions, intents, and file saving)
 * using mocked Context, Intent, and DocumentFile objects.
 */

@RunWith(RobolectricTestRunner::class)
class SAFHelperTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver

    /**
     * Sets up mock Context and ContentResolver before each test.
     * We need this because we have to check if we have already
     * write permission before saving files to a folder.
     * */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = mockk(relaxed = true)
        resolver = mockk(relaxed = true)
        every { context.contentResolver } returns resolver
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `hasWritePermission returns true when permission exists`() {
        val uri = mockk<Uri>()
        val permission = mockk<android.content.UriPermission>()
        every { permission.uri } returns uri
        every { permission.isWritePermission } returns true
        every { resolver.persistedUriPermissions } returns listOf(permission)

        assertTrue(SAFHelper.hasWritePermission(context, uri))
    }

    @Test
    fun `hasWritePermission returns false when permission not found`() {
        val uri = mockk<Uri>()
        every { resolver.persistedUriPermissions } returns emptyList()

        assertFalse(SAFHelper.hasWritePermission(context, uri))
    }

    @Test
    fun `createFolderPickerIntent returns proper intent`() {
        val intent = SAFHelper.createFolderPickerIntent()

        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, intent.action)
        val flags = intent.flags
        assertTrue(flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertTrue(flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
        assertTrue(flags and Intent.FLAG_GRANT_PREFIX_URI_PERMISSION != 0)
    }

    @Test
    fun `persistFolderPermission calls takePersistableUriPermission`() {
        val uri = mockk<Uri>()
        val intent = mockk<Intent>()
        every { intent.data } returns uri

        SAFHelper.persistFolderPermission(context, intent)

        verify {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    @Test
    fun `persistFolderPermission does nothing if intent or uri is null`() {
        SAFHelper.persistFolderPermission(context, null)
        SAFHelper.persistFolderPermission(context, Intent())
        verify(exactly = 0) { resolver.takePersistableUriPermission(any(), any()) }
    }


    @Test
    fun `saveFilesToFolder copies all files successfully`() = runTest {
        mockkStatic(DocumentFile::class)

        val folderUri = mockk<Uri>()
        val folder = mockk<DocumentFile>()
        val sourceUri = mockk<Uri>()
        val newFile = mockk<DocumentFile>()

        every { DocumentFile.fromTreeUri(context, folderUri) } returns folder
        every { folder.createFile(any(), any()) } returns newFile

        val input = ByteArrayInputStream("data".toByteArray())
        val output = ByteArrayOutputStream()

        every { resolver.openInputStream(sourceUri) } returns input
        every { resolver.openOutputStream(newFile.uri) } returns output
        every { context.getString(any(), any()) } returns "file_name"

        val count = SAFHelper.saveFilesToFolder(context, folderUri, listOf(sourceUri))

        assertEquals(1, count)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun `saveFilesToFolder returns zero when folder is null`() = runTest {
        mockkStatic(DocumentFile::class)
        val folderUri = mockk<Uri>()
        every { DocumentFile.fromTreeUri(context, folderUri) } returns null

        val count = SAFHelper.saveFilesToFolder(context, folderUri, emptyList())

        assertEquals(0, count)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun `saveFilesToFolder handles IOException gracefully`() = runTest {
        mockkStatic(DocumentFile::class)

        val folderUri = mockk<Uri>()
        val folder = mockk<DocumentFile>()
        val sourceUri = mockk<Uri>()

        every { DocumentFile.fromTreeUri(context, folderUri) } returns folder
        every { folder.createFile(any(), any()) } throws IOException("fail")
        every { context.getString(any(), any()) } returns "file_name"

        val count = SAFHelper.saveFilesToFolder(context, folderUri, listOf(sourceUri))

        assertEquals(0, count)
        unmockkStatic(DocumentFile::class)
    }
}