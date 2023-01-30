package net.gini.android.bank.sdk.capture.digitalinvoice.help

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.bank.sdk.R
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.help.PhotoTipsAdapter
import net.gini.android.capture.internal.util.ActivityHelper

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gbs_activity_help)
        if (!GiniCapture.hasInstance()) {
            finish()
            return
        }

        ActivityHelper.forcePortraitOrientationOnPhones(this)

        setupHelpList()
    }

    private fun setupHelpList() {
        val recyclerView = findViewById<RecyclerView>(R.id.gbs_help_items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HelpItemAdapter(this)
    }
}
