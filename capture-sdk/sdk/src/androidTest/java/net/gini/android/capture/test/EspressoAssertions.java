package net.gini.android.capture.test;


import androidx.test.espresso.ViewAssertion;

public class EspressoAssertions {

    public static ViewAssertion hasSizeRatio(final float sizeRatio) {
        return new ViewSizeRatioAssertion(sizeRatio);
    }
}
