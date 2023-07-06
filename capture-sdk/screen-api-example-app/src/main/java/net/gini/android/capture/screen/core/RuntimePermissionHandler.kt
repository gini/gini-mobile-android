package net.gini.android.capture.screen.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import org.slf4j.LoggerFactory

/**
 *
 */
class RuntimePermissionHandler private constructor(builder: Builder) {
    private val mActivity: Activity?
    private val mCameraPermissionDeniedMessage: String?
    private val mCameraPermissionRationale: String?
    private val mCancelButtonTitle: String?
    private val mGrantAccessButtonTitle: String?
    private val mStoragePermissionDeniedMessage: String?
    private val mStoragePermissionRationale: String?

    init {
        mActivity = builder.mActivity
        mStoragePermissionDeniedMessage = builder.mStoragePermissionDeniedMessage
        mStoragePermissionRationale = builder.mStoragePermissionRationale
        mCameraPermissionDeniedMessage = builder.mCameraPermissionDeniedMessage
        mCameraPermissionRationale = builder.mCameraPermissionRationale
        mGrantAccessButtonTitle = builder.mGrantAccessButtonTitle
        mCancelButtonTitle = builder.mCancelButtonTitle
    }

    fun requestCameraPermission(listener: Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            listener.permissionGranted()
            return
        }
        Dexter.withActivity(mActivity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    listener.permissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    showCameraPermissionDeniedDialog(listener)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    showCameraPermissionRationale(token)
                }
            })
            .withErrorListener { error -> LOG.error("Permission error: {}", error.name) }
            .check()
    }

    private fun showCameraPermissionDeniedDialog(listener: Listener) {
        val alertDialog = MaterialAlertDialogBuilder(mActivity!!)
            .setMessage(mCameraPermissionDeniedMessage)
            .setPositiveButton(mGrantAccessButtonTitle) { dialog, which -> showAppDetailsSettingsScreen() }
            .setNegativeButton(mCancelButtonTitle, null)
            .setOnCancelListener { listener.permissionDenied() }
            .create()
        alertDialog.show()
    }

    private fun showAppDetailsSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", mActivity!!.packageName, null)
        intent.data = uri
        mActivity.startActivity(intent)
    }

    private fun showCameraPermissionRationale(token: PermissionToken) {
        val alertDialog = MaterialAlertDialogBuilder(mActivity!!)
            .setMessage(mCameraPermissionRationale)
            .setPositiveButton(
                mGrantAccessButtonTitle
            ) { dialog, which -> token.continuePermissionRequest() }
            .setOnCancelListener { token.cancelPermissionRequest() }
            .create()
        alertDialog.show()
    }

    @SuppressLint("InlinedApi")
    fun requestStoragePermission(listener: Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            listener.permissionGranted()
            return
        }
        Dexter.withActivity(mActivity)
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    listener.permissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    showStoragePermissionDeniedDialog(listener)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    showStoragePermissionRationale(token)
                }
            })
            .withErrorListener { error -> LOG.error("Permission error: {}", error.name) }
            .check()
    }

    private fun showStoragePermissionDeniedDialog(listener: Listener) {
        val alertDialog = MaterialAlertDialogBuilder(mActivity!!)
            .setMessage(mStoragePermissionDeniedMessage)
            .setPositiveButton(
                mGrantAccessButtonTitle
            ) { dialog, which -> showAppDetailsSettingsScreen() }
            .setNegativeButton(mCancelButtonTitle) { dialog, which -> listener.permissionDenied() }
            .setOnCancelListener { listener.permissionDenied() }
            .create()
        alertDialog.show()
    }

    private fun showStoragePermissionRationale(token: PermissionToken) {
        val alertDialog = MaterialAlertDialogBuilder(mActivity!!)
            .setMessage(mStoragePermissionRationale)
            .setPositiveButton(
                mGrantAccessButtonTitle
            ) { dialog, which -> token.continuePermissionRequest() }
            .setOnCancelListener { token.cancelPermissionRequest() }
            .create()
        alertDialog.show()
    }

    interface Listener {
        fun permissionDenied()
        fun permissionGranted()
    }



    data class Builder(
        var mActivity: Activity? = null,
        var mCameraPermissionDeniedMessage: String? = null,
        var mCameraPermissionRationale: String? = null,
        var mCancelButtonTitle: String? = null,
        var mGrantAccessButtonTitle: String? = null,
        var mStoragePermissionDeniedMessage: String? = null,
        var mStoragePermissionRationale: String? = null
    ) {

        fun build(): RuntimePermissionHandler {
            return RuntimePermissionHandler(this)
        }

        fun withCameraPermissionDeniedMessage(
            cameraPermissionDeniedMessage: String?
        ): Builder {
            mCameraPermissionDeniedMessage = cameraPermissionDeniedMessage
            return this
        }

        fun withCameraPermissionRationale(cameraPermissionRationale: String?): Builder {
            mCameraPermissionRationale = cameraPermissionRationale
            return this
        }

        fun withCancelButtonTitle(cancelButtonTitle: String?): Builder {
            mCancelButtonTitle = cancelButtonTitle
            return this
        }

        fun withGrantAccessButtonTitle(grantAccessButtonTitle: String?): Builder {
            mGrantAccessButtonTitle = grantAccessButtonTitle
            return this
        }

        fun withStoragePermissionDeniedMessage(
            storagePermissionDeniedMessage: String?
        ): Builder {
            mStoragePermissionDeniedMessage = storagePermissionDeniedMessage
            return this
        }

        fun withStoragePermissionRationale(storagePermissionRationale: String?): Builder {
            mStoragePermissionRationale = storagePermissionRationale
            return this
        }

        fun forActivity(activity: Activity): Builder {
            mActivity = activity
            return this
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(
            RuntimePermissionHandler::class.java
        )

        fun forActivity(activity: Activity): Builder {
            return Builder().forActivity(activity)
        }
    }
}