package net.gini.android.capture.help

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.help.HelpItem.Custom
import net.gini.android.capture.help.HelpItem.PhotoTips
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 */
class HelpActivity : AppCompatActivity() {

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gc_activity_help)
        if (!GiniCapture.hasInstance()) {
            finish()
            return
        }
        setUpHelpItems()
        ActivityHelper.forcePortraitOrientationOnPhones(this)
        if (hasOnlyOneHelpItem()) {
            launchHelpScreen((mRecyclerView.adapter as HelpItemsAdapter?)!!.items[0])
            finish()
        }
        setupBottomBarNavigation()
        setupTopBarNavigation()
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer =
            findViewById<InjectedViewContainer<NavigationBarTopAdapter>>(R.id.gc_injected_navigation_bar_container_top)
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(
                    GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
                ) { injectedViewAdapter ->
                    val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                        NavButtonType.NONE else NavButtonType.BACK
                    injectedViewAdapter.setNavButtonType(navType)
                    injectedViewAdapter.setTitle(getString(R.string.gc_title_help))
                    injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                        onBackPressed()
                    })
                }
        }
    }


    private fun setupBottomBarNavigation() {
        val injectedViewContainer: InjectedViewContainer<HelpNavigationBarBottomAdapter>? =
            findViewById(R.id.gc_injected_navigation_bar_container_bottom)
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            injectedViewContainer?.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().helpNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    onBackPressed()
                })
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpHelpItems() {
        mRecyclerView = findViewById<RecyclerView>(R.id.gc_help_items).apply {
            layoutManager = LinearLayoutManager(this@HelpActivity)
            adapter = HelpItemsAdapter { helpItem -> launchHelpScreen(helpItem) }
        }
    }

    private fun hasOnlyOneHelpItem(): Boolean {
        return mRecyclerView.adapter!!.itemCount == 1
    }

    private fun launchHelpScreen(helpItem: HelpItem) {
        when (helpItem) {
            PhotoTips -> startActivityForResult(
                Intent(this, helpItem.activityClass),
                PHOTO_TIPS_REQUEST
            )
            is Custom -> startActivity(helpItem.intent)
            else -> startActivity(Intent(this, helpItem.activityClass))
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_TIPS_REQUEST
            && resultCode == PhotoTipsActivity.RESULT_SHOW_CAMERA_SCREEN
        ) {
            finish()
        }
    }


    companion object {
        private const val PHOTO_TIPS_REQUEST = 1
    }
}