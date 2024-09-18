package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.R

class InvoicePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_preview)
        val documentId =
            intent.getStringExtra(EXTRA_DOCUMENT_ID) ?: error("Missing $EXTRA_DOCUMENT_ID extra")

        val fragment = GiniBank.createInvoicePreviewFragment(documentId)

        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
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