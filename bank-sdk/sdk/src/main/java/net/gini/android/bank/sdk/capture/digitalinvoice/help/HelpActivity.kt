package net.gini.android.bank.sdk.capture.digitalinvoice.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.help.view.DigitalInvoiceHelpNavigationBarBottomAdapter
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 *
 */
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
        setupTopBarNavigation()
        setupBottomNavigationBar()
        handleOnBackPressed()
    }

    private fun setupHelpList() {
        val recyclerView = findViewById<RecyclerView>(R.id.gbs_help_items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HelpItemAdapter(this)
    }

    private fun handleOnBackPressed() {
        ActivityHelper.interceptOnBackPressed(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer =
            findViewById<InjectedViewContainer<NavigationBarTopAdapter>>(R.id.gbs_injected_navigation_bar_container_top)
        if (GiniCapture.hasInstance()) {

            topBarInjectedViewContainer.injectedViewAdapter =
                GiniCapture.getInstance().navigationBarTopAdapter

            val topBarAdapter = topBarInjectedViewContainer?.injectedViewAdapter

            val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                NavButtonType.NONE else NavButtonType.BACK
            topBarAdapter?.setNavButtonType(navType)

            topBarAdapter?.setTitle(getString(net.gini.android.capture.R.string.gc_title_help))

            topBarAdapter?.setOnNavButtonClickListener(IntervalClickListener {
                onBackPressed()
            })
        }
    }

    private fun setupBottomNavigationBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            val injectedViewContainer =
                findViewById<InjectedViewContainer<DigitalInvoiceHelpNavigationBarBottomAdapter>>(R.id.gbs_injected_navigation_bar_container_bottom)
            val adapter = GiniBank.digitalInvoiceHelpNavigationBarBottomAdapter
            injectedViewContainer.injectedViewAdapter = adapter
            adapter.setOnBackButtonClickListener(IntervalClickListener {
                onBackPressed()
            })
        }
    }
}
