package net.gini.android.capture.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.gini.android.capture.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * Internal use only.
 * SAFHelper
 *
 * This object helps with saving files using Android's Storage Access Framework (SAF).
 * It checks write permissions, opens a folder picker if needed, remembers folder access,
 * and saves one or more files into the selected folder.
 *
 * Used when the app needs to let the user choose a folder and save files.
 */

internal object SAFHelper {

    /**
    * Checks if the app has write permission for the given folder URI.
    *
    * @param context The context used to access the content resolver.
    * @param folderUri The URI of the folder to check.
    * @return True if write permission exists, false otherwise.
    */

    fun hasWritePermission(context: Context, folderUri: Uri): Boolean {
        if (directoryExists(context, folderUri).not()) return false
        val result = context.contentResolver.persistedUriPermissions.any {
            it.uri == folderUri && it.isWritePermission
        }
        return result
    }

    /**
     * Checks if the directory exists and is accessible. Helps in case of
     * user deletes the whole directory, then we have to ask for permission again.
     *
     * @param context The context used to access the content resolver.
     * @param folderUri The URI of the folder to check.
     * @return True if the directory exists and is accessible, false otherwise.
     */
    private fun directoryExists(context: Context, folderUri: Uri): Boolean {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            documentFile?.exists() == true && documentFile.isDirectory
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Creates an intent that opens a system folder picker for the user.
     * The intent requests both read and write access to the chosen folder.
     *
     * @return Intent ready to start with startActivityForResult() or ActivityResultLauncher.
     */

    fun createFolderPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
    }

    /**
     * Saves the user's folder selection permission so it can be reused later
     * without asking again.
     *
     * @param context The context used to access the content resolver.
     * @param dataIntent The intent returned from the folder picker.
     */

    fun persistFolderPermission(context: Context, dataIntent: Intent?) {
        if (dataIntent == null) return
        val folderUri = dataIntent.data ?: return
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(folderUri, takeFlags)
        } catch (_: SecurityException) {
            // no-op
        }
    }

    /**
     * Saves multiple files to the selected folder.
     *
     * @param context The context used to access files and resolver.
     * @param folderUri The URI of the destination folder.
     * @param sourceUris List of URIs of the source files to copy.
     * @return Number of files successfully saved.
     */

    fun saveFilesToFolder(
        context: Context,
        folderUri: Uri,
        sourceUris: List<Uri>,
    ): Int = runBlocking {
        withContext(Dispatchers.IO) {
            val pickedDir = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext 0

            val results = sourceUris.mapIndexed { suffixForFileName, uri ->
                val fileName = context.getString(
                    R.string.gc_invoice_file_name,
                    System.currentTimeMillis(), suffixForFileName
                )
                async { saveSingleFile(context, pickedDir, uri, fileName) }
            }.awaitAll()

            results.count { it }
        }
    }

    /**
     * Copies one file at a time to the given folder.
     *
     * @param context The context used to open streams.
     * @param folder The DocumentFile representing the target folder.
     * @param sourceUri The URI of the source file.
     * @return True if the file was saved successfully, false otherwise.
     */

    private fun saveSingleFile(
        context: Context,
        folder: DocumentFile,
        sourceUri: Uri,
        fileName: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val newFile = folder.createFile("image/jpeg", fileName)
            val input = resolver.openInputStream(sourceUri)
            val output = newFile?.let { resolver.openOutputStream(it.uri) }

            val success = if (newFile != null && input != null && output != null) {
                copyStreams(input, output)
                true
            } else false

            success
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun copyStreams(input: InputStream, output: OutputStream) {
        input.use { i -> output.use { o -> i.copyTo(o) } }
    }
}
