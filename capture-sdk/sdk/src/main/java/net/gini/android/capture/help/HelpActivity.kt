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
import net.gini.android.capture.internal.util.ActivityHelper

/**
 * <h3>Screen API and Component API</h3>
 *
 * <p>
 * On the Help Screen users can get information about how to best use the Gini Capture SDK.
 * </p>
 * <p>
 * This Activity can be used for both Screen and Component APIs.
 * </p>
 *
 * <h3>Customizing the Help Screen</h3>
 *
 * <p>
 * Customizing the look of the Help Screen is done via overriding of app resources.
 * </p>
 * <p>
 * The following items are customizable:
 * <ul>
 *     <li>
 *         <b>Background color:</b> via the color resource named {@code gc_help_activity_background}
 *     </li>
 *     <li>
 *         <b>Help list item background color:</b> via the color resource name {@code gc_help_item_background}
 *     </li>
 *     <li>
 *         <b>Help list item text style:</b> via overriding the style named {@code
 *         GiniCaptureTheme.Help.Item.TextStyle}
 *     </li>
 *     <li>
 *         <b>Help list item labels:</b> via overriding the string resources found in the {@link DefaultHelpItem} enum
 *     </li>
 * </ul>
 * </p>
 *
 * <p>
 *     <b>Important:</b> All overriden styles must have their respective {@code Root.} prefixed style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Onboarding.Message.TextStyle} must be {@code Root.GiniCaptureTheme.Onboarding.Message.TextStyle}.
 * </p>
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * <p>
 * Customizing the Action Bar is done via overriding of app resources and each one - except the
 * title string resource - is global to all Activities ({@link CameraActivity}, {@link
 * NoResultsActivity}, {@link HelpActivity}, {@link ReviewActivity}, {@link AnalysisActivity}).
 * </p>
 * <p>
 * The following items are customizable:
 * <ul>
 * <li>
 * <b>Background color:</b> via the color resource named {@code gc_action_bar} (highly recommended
 * for Android 5+: customize the status bar color via {@code gc_status_bar})
 * </li>
 * <li>
 * <b>Title:</b> via the string resource name {@code gc_title_help}
 * </li>
 * <li>
 * <b>Title color:</b> via the color resource named {@code gc_action_bar_title}
 * </li>
 * <li><b>Back button:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 * {@code gc_action_bar_back}
 * </li>
 * </ul>
 * </p>
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
        setupHomeButton()
    }

    private fun setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            ActivityHelper.enableHomeAsUp(this)
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