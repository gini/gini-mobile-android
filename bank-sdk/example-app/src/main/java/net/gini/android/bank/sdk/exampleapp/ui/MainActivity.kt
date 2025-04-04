package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityMainBinding

/**
 * Entry point for the screen api example app.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            handleOpenWithIntent(intent)
        }
    }

    private fun handleOpenWithIntent(intent: Intent?) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_content) as NavHostFragment

        val navController = navHostFragment.navController

        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)

        val isFileIntent = intent?.action == Intent.ACTION_VIEW ||
                intent?.action == Intent.ACTION_SEND ||
                intent?.action == Intent.ACTION_SEND_MULTIPLE



        if (isFileIntent) {
            graph.setStartDestination(R.id.clientBankSDKFragment)
            navController.setGraph(
                graph, bundleOf(
                    "fileIntent" to intent  // Pass full intent or extract Uri & pass
                )
            )
            navController.navigate(
                R.id.clientBankSDKFragment,
                bundleOf(
                    "fileIntent" to intent  // Pass full intent or extract Uri & pass
                ),
                NavOptions.Builder().apply {
                    setPopUpTo(R.id.clientBankSDKFragment, false)
                }.build()
            )
        } else {
            navController.graph = graph
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenWithIntent(intent)
    }


    companion object {
        const val CONFIGURATION_BUNDLE = "CONFIGURATION_BUNDLE"
        const val CAMERA_PERMISSION_BUNDLE = "CAMERA_PERMISSION_BUNDLE"
        const val EXTRA_IN_OPEN_WITH_DOCUMENT = "EXTRA_IN_OPEN_WITH_DOCUMENT"
        private const val REQUEST_CONFIGURATION = 3
    }
}