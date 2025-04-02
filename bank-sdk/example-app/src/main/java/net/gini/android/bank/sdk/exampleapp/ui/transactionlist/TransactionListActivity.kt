package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityInvoicePreviewBinding

@AndroidEntryPoint
class TransactionListActivity : AppCompatActivity() {

    lateinit var binding: ActivityInvoicePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(R.navigation.tl_demo_nav_graph,)
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
            return Intent(context, TransactionListActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_TITLE, screenTitle)
                putExtra(EXTRA_DOCUMENT_ID, documentId)
                putExtra(EXTRA_INFO_TEXT_LINES, infoTextLines.toTypedArray())
            }
        }
    }
}
