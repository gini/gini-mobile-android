package net.gini.android.capture;

import net.gini.android.capture.noresults.NoResultsFragmentCompat;

/**
 * <p>
 * Interface used by {@link NoResultsFragmentCompat} and (@link ErrorFragmentCompat} to dispatch events to the hosting Activity.
 * </p>
 */
public interface ImageRetakeOptionsListener {

    /**
     * <p>
     *     Called when the button on the bottom of the No Results/Error Screen was pressed. This button
     *     should lead the user back to the Camera Screen.
     * </p>
     */
    void onBackToCameraPressed();

    /**
     * <p>
     *     Called when the button on the bottom of the No Results/Error Screen was pressed. This button
     *     should lead the user to the manual data input form/site.
     * </p>
     */
    void onEnterManuallyPressed();
}
