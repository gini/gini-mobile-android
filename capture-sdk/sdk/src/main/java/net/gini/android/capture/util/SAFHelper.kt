package net.gini.android.capture.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.gini.android.capture.R
import net.gini.android.capture.internal.textrecognition.MLKitTextRecognizer.Companion.LOG
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

    fun hasWritePermission(context: Context, folderUri: Uri): Boolean {
        logDebug("hasWritePermission: Checking permissions for $folderUri")
        val result = context.contentResolver.persistedUriPermissions.any {
            it.uri == folderUri && it.isWritePermission
        }
        logDebug("hasWritePermission: result = $result")
        return result
    }

    fun createFolderPickerIntent(): Intent {
        logDebug("createFolderPickerIntent: Creating intent for folder picker")
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
    }

    fun persistFolderPermission(context: Context, dataIntent: Intent?) {
        logDebug("persistFolderPermission: called")
        if (dataIntent == null) {
            logDebug("persistFolderPermission: dataIntent is null")
            return
        }
        val folderUri = dataIntent.data
        if (folderUri == null) {
            logDebug("persistFolderPermission: folderUri is null")
            return
        }

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            logDebug("persistFolderPermission: taking permission for $folderUri")
            context.contentResolver.takePersistableUriPermission(folderUri, takeFlags)
            logDebug("persistFolderPermission: permission granted successfully")
        } catch (e: SecurityException) {
            logDebug("persistFolderPermission exception = ${e.message}")
            LOG.error("SecurityException in SAF", e)
        }
    }

    fun saveFilesToFolder(
        context: Context,
        folderUri: Uri,
        sourceUris: List<Uri>,
    ): Int = runBlocking {
        logDebug("saveFilesToFolder: Starting to save ${sourceUris.size} files")
        withContext(Dispatchers.IO) {
            val pickedDir = DocumentFile.fromTreeUri(context, folderUri)
            if (pickedDir == null) {
                logDebug("saveFilesToFolder: pickedDir is null, returning 0")
                return@withContext 0
            }

            val results = sourceUris.mapIndexed {index, uri ->
//                val fileName = context.getString(R.string.gc_invoice_file_name, System.currentTimeMillis())
                val fileName = context.getString(R.string.gc_invoice_file_name, System.currentTimeMillis(),index)
                logDebug("saveFilesToFolder: Preparing to save $uri as $fileName")
                async { saveSingleFile(context, pickedDir, uri, fileName) }
            }.awaitAll()

            val count = results.count { it }
            logDebug("saveFilesToFolder: Completed. Successfully saved $count / ${sourceUris.size} files")
            count
        }
    }

    private fun saveSingleFile(
        context: Context,
        folder: DocumentFile,
        sourceUri: Uri,
        fileName: String
    ): Boolean {
        logDebug("saveSingleFile: Called for $fileName from $sourceUri")
        return try {
            val resolver = context.contentResolver

            logDebug("saveSingleFile: Creating target file $fileName")
            val newFile = folder.createFile("image/jpeg", fileName)
            logDebug("saveSingleFile: newFile uri = ${newFile?.uri}")

            logDebug("saveSingleFile: Opening input stream for $sourceUri")
            val input = resolver.openInputStream(sourceUri)
            logDebug("saveSingleFile: input stream = ${input != null}")

            logDebug("saveSingleFile: Opening output stream for ${newFile?.uri}")
            val output = newFile?.let { resolver.openOutputStream(it.uri) }
            logDebug("saveSingleFile: output stream = ${output != null}")

            val success = if (newFile != null && input != null && output != null) {
                logDebug("saveSingleFile: Starting copyStreams()")
                copyStreams(input, output)
                logDebug("saveSingleFile: Finished copyStreams() successfully")
                true
            } else {
                logDebug("saveSingleFile: One or more streams are null (newFile=$newFile, input=$input, output=$output)")
                false
            }

            logDebug("saveSingleFile: Success = $success for file $fileName")
            success
        } catch (e: IOException) {
            LOG.error("IOException in SAF", e)
            logDebug("saveSingleFile IOException: ${e.message}")
            false
        } catch (e: SecurityException) {
            LOG.error("SecurityException in SAF", e)
            logDebug("saveSingleFile SecurityException: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            LOG.error("IllegalArgumentException in SAF", e)
            logDebug("saveSingleFile IllegalArgumentException: ${e.message}")
            false
        }
    }

    private fun copyStreams(input: InputStream, output: OutputStream) {
        logDebug("copyStreams: Starting copy")
        input.use { i ->
            output.use { o ->
                val bytesCopied = i.copyTo(o)
                logDebug("copyStreams: Copied $bytesCopied bytes")
            }
        }
        logDebug("copyStreams: Completed")
    }

    fun logDebug(message: String) {
        Log.d("SavingInvoicesLocally", "message = $message")
    }
}
