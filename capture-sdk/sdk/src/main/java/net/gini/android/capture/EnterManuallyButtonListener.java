package net.gini.android.capture;

import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.noresults.NoResultsFragmentCompat;
/**
 * <p>
 * Interface used by {@link NoResultsFragmentCompat} and {@link ErrorFragment} to dispatch events to the hosting Activity.
 * </p>
 */
public interface EnterManuallyButtonListener {


    /**
     * <p>
     *     Called when the button on the bottom of the No Results/Error Screen was pressed. This button
     *     should lead the user to the manual data input form/site.
     * </p>
     */
    void onEnterManuallyPressed();
}
