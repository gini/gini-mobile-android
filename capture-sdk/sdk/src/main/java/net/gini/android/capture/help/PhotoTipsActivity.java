package net.gini.android.capture.help;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.internal.util.FeatureConfiguration;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.review.ReviewActivity;

import androidx.appcompat.app.AppCompatActivity;

/**
 * <h3>Screen API and Component API</h3>
 *
 * <p>
 *     On the Photo Tips Screen users can get information about how to take better pictures.
 * </p>
 * <p>
 *     This Activity is launched by the {@link HelpActivity} for both Screen and Component APIs.
 * </p>
 *
 * <h3>Customizing the Photo Tips Screen</h3>
 *
 * <p>
 *     Customizing the look of the Photo Tips Screen is done via overriding of app resources.
 * </p>
 * <p>
 *     The following items are customizable:
 *     <ul>
 *         <li>
 *             <b>Background color:</b> via the color resource named {@code gc_photo_tips_activity_background}.
 *         </li>
 *         <li>
 *             <b>Header text style:</b> via overriding the style named {@code GiniCaptureTheme.Help.PhotoTips.Header.TextStyle}
 *         </li>
 *         <li>
 *             <b>Tip text style:</b> via overriding the style named {@code GiniCaptureTheme.Help.PhotoTips.Tip.TextStyle}
 *         </li>
 *         <li>
 *             <b>Tip image - Good lighting:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
 *             named
 *             {@code gc_photo_tip_lighting.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Document should be flat:</b> via images for mdpi, hdpi, xhdpi, xxhdpi,
 *             xxxhdpi
 *             named {@code gc_photo_tip_flat.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Device should be parallel to document:</b> via images for mdpi, hdpi,
 *             xhdpi,xxhdpi, xxxhdpi named {@code gc_photo_tip_parallel.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Document should be aligned with corner guides:</b> via
 *             images for mdpi, hdpi, xhdpi,xxhdpi, xxxhdpi named {@code gc_photo_tip_align.png}
 *         </li>
 *         <li>
 *             <b>Button color:</b> via the color resource named {@code gc_photo_tips_button}
 *         </li>
 *         <li>
 *             <b>Button text color:</b> via the color resource named {@code gc_photo_tips_button_text}
 *         </li>
 *     </ul>
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
 * <b>Title:</b> via the string resource name {@code gc_title_photo_tips}
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
public class PhotoTipsActivity extends AppCompatActivity {

    static final int RESULT_SHOW_CAMERA_SCREEN = RESULT_FIRST_USER + 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }
        if (FeatureConfiguration.isMultiPageEnabled()) {
            setContentView(R.layout.gc_activity_photo_tips_with_multipage);
        } else {
            setContentView(R.layout.gc_activity_photo_tips);
        }
        findViewById(R.id.gc_button_photo_tips_camera).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        setResult(RESULT_SHOW_CAMERA_SCREEN);
                        finish();
                    }
                });
        forcePortraitOrientationOnPhones(this);
        setupHomeButton();
    }

    private void setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            enableHomeAsUp(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
