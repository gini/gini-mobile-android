package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityInvoicePreviewBinding

@AndroidEntryPoint
class TransactionDocsActivity : AppCompatActivity() {

    lateinit var binding: ActivityInvoicePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(R.navigation.tl_demo_nav_graph)
    }
}
