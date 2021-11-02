package net.gini.pay.appscreenapi.util

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PermissionHandler(private val activity: ComponentActivity) {
    private var permissionContinuation: Continuation<Boolean>? = null

    private var requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            permissionContinuation?.resume(true)
        } else {
            permissionContinuation?.resume(false)
        }
        permissionContinuation = null
    }

    suspend fun grantPermission(permission: String): Boolean = if (permissionContinuation == null) {
        suspendCancellableCoroutine { continuation ->
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                continuation.resume(true)
            } else {
                permissionContinuation = continuation
                requestPermissionLauncher.launch(permission)
            }
        }
    } else {
        false
    }
}