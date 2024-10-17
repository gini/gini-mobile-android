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
        val infoTextLines =
            intent.getStringArrayExtra(EXTRA_INFO_TEXT_LINES)?.toList() ?: emptyList()
        val screenTitle =
            intent.getStringExtra(EXTRA_SCREEN_TITLE) ?: error("Missing $EXTRA_SCREEN_TITLE extra")

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(
            R.navigation.invoice_preview_nav_graph,
            GiniBank.createInvoicePreviewFragmentArgs(screenTitle, documentId, infoTextLines).toBundle()
        )
    }

    companion object {

        private const val EXTRA_DOCUMENT_ID = "document_id"
        private const val EXTRA_INFO_TEXT_LINES = "info_text_lines"
        private const val EXTRA_SCREEN_TITLE = "screen_title"

        fun newIntent(
            context: Context,
            screenTitle: String,
            documentId: String,
            infoTextLines: List<String>
        ): Intent {
            return Intent(context, InvoicePreviewActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_TITLE, screenTitle)
                putExtra(EXTRA_DOCUMENT_ID, documentId)
                putExtra(EXTRA_INFO_TEXT_LINES, infoTextLines.toTypedArray())
            }
        }
    }
}
