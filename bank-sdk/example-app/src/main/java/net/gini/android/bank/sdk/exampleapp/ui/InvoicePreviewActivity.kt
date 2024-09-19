package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityInvoicePreviewBinding

@AndroidEntryPoint
class InvoicePreviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityInvoicePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val documentId =
            intent.getStringExtra(EXTRA_DOCUMENT_ID) ?: error("Missing $EXTRA_DOCUMENT_ID extra")

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(
            R.navigation.invoice_preview_nav_graph,
            GiniBank.createInvoicePreviewFragmentArgs(documentId).toBundle()
        )
    }

    companion object {

        private const val EXTRA_DOCUMENT_ID = "document_id"

        fun newIntent(context: Context, documentId: String): Intent {
            return Intent(context, InvoicePreviewActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_ID, documentId)
            }
        }
    }
}
