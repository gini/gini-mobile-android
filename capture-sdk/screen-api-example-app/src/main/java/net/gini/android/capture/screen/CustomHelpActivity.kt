package net.gini.android.capture.screen

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class CustomHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_help)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}