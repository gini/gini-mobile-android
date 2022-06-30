package net.gini.android.capture.onboarding;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import androidx.annotation.DrawableRes;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;

public class PageIndicatorImageViewSubject extends Subject {

    public static Factory<PageIndicatorImageViewSubject, ImageView> pageIndicatorImageView() {
        return new Factory<PageIndicatorImageViewSubject, ImageView>() {
            @Override
            public PageIndicatorImageViewSubject createSubject(FailureMetadata metadata, ImageView actual) {
                return new PageIndicatorImageViewSubject(metadata, actual);
            }
        };
    }

    private final ImageView actual;

    protected PageIndicatorImageViewSubject(FailureMetadata metadata, @Nullable Object actual) {
        super(metadata, actual);
        isNotNull();
        this.actual = (ImageView) actual;
    }

    public void showsDrawable(@DrawableRes final int drawableResId) {
        final ImageView imageView = actual;

        final BitmapDrawable expectedDrawable = (BitmapDrawable) imageView.getResources().getDrawable(
                drawableResId);
        if (expectedDrawable == null || expectedDrawable.getBitmap() == null) {
            failWithActual(fact("shows drawable with id", drawableResId), simpleFact("no such drawable"));
        }
        // NullPointerException warning is not relevant, fail() above will prevent it
        //noinspection ConstantConditions
        final Bitmap expectedBitmap = expectedDrawable.getBitmap();

        final BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        final Bitmap bitmap = bitmapDrawable.getBitmap();

        if (!bitmap.sameAs(expectedBitmap)) {
            failWithActual(fact("shows drawable with id", drawableResId));
        }
    }
}
