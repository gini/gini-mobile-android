package net.gini.android.bank.sdk.capture.util

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import org.slf4j.LoggerFactory

fun Fragment.safeNavigate(
    navController: NavController,
    navDirections: NavDirections,
)  {
    try {
        navController.navigate(navDirections)
    } catch (exception: java.lang.Exception) {
        val logger = LoggerFactory.getLogger(this::class.java)
        logger.error("Navigation exception " + exception.message)
    }
}