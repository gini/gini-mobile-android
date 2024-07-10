package net.gini.android.capture.tracking.useranalytics.tracker

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import org.slf4j.LoggerFactory
import java.util.Locale

class DeviceInfo(private val context: Context) {

    private var cachedInfo: CachedInfo? = null
        get() {
            if (field == null) {
                field = CachedInfo()
            }
            return field
        }

    private val LOG = LoggerFactory.getLogger(DeviceInfo::class.java)

    /**
     * Internal class serves as a cache
     */
    inner class CachedInfo {
        val versionName: String?
        val osName: String
        val osVersion: String
        val brand: String
        val manufacturer: String
        val model: String
        val carrier: String?
        val language: String

        init {
            versionName = fetchVersionName()
            osName = OS_NAME
            osVersion = fetchOsVersion()
            brand = fetchBrand()
            manufacturer = fetchManufacturer()
            model = fetchModel()
            carrier = fetchCarrier()
            language = fetchLanguage()
        }

        /**
         * Internal methods for getting raw information
         */
        private fun fetchVersionName(): String? {
            val packageInfo: PackageInfo
            try {
                packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
            } catch (e: Exception) {
            }
            return null
        }

        private fun fetchOsVersion(): String {
            return Build.VERSION.RELEASE
        }

        private fun fetchBrand(): String {
            return Build.BRAND
        }

        private fun fetchManufacturer(): String {
            return Build.MANUFACTURER
        }

        private fun fetchModel(): String {
            return Build.MODEL
        }

        private fun fetchCarrier(): String? {
            try {
                val manager = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return manager.networkOperatorName
            } catch (e: Exception) {
                // Failed to get network operator name from network
            }
            return null
        }


        private val locale: Locale
            get() {
                val configuration = Resources.getSystem().configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = configuration.locales
                    if (localeList.isEmpty) {
                        return Locale.getDefault()
                    } else {
                        return localeList.get(0)
                    }
                } else {
                    return configuration.locale
                }
            }

        private fun fetchLanguage(): String {
            return locale.language
        }

    }

    fun prefetch() {
        cachedInfo
    }


    val versionName: String?
        get() = cachedInfo!!.versionName
    val osName: String
        get() = cachedInfo!!.osName
    val osVersion: String
        get() = cachedInfo!!.osVersion
    val brand: String
        get() = cachedInfo!!.brand
    val manufacturer: String
        get() = cachedInfo!!.manufacturer
    val model: String
        get() = cachedInfo!!.model
    val carrier: String?
        get() = cachedInfo!!.carrier
    val language: String
        get() = cachedInfo!!.language

    companion object {
        const val OS_NAME = "android"
    }
}