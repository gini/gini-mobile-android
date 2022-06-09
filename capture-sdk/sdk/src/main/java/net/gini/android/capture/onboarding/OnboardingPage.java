package net.gini.android.capture.onboarding;

import android.os.Parcel;
import android.os.Parcelable;

import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * <p>
 *     The {@code OnboardingPage} is used by the Onboarding Fragment to display an image and a short text.
 * </p>
 * <p>
 *     Use this class to show a different number of pages in the Onboarding Screen. Customizing the default onboarding pages can be done via overriding of app resources.
 * </p>
 * <p>
 *     When using the Screen API set an {@link java.util.ArrayList} containing {@code OnboardingPage} objects as the {@link CameraActivity#EXTRA_IN_ONBOARDING_PAGES} when starting the {@link CameraActivity}.
 * </p>
 * <p>
 *     When using the Componenent API provide an {@link java.util.ArrayList} containing {@code OnboardingPage} objects as the argument for the Onboarding Fragment factory method {@link OnboardingFragment#createInstance(ArrayList)}.
 * </p>
 */
public class OnboardingPage implements Parcelable {

    private final int titleResId;
    private final int messageResId;
    private OnboardingIllustrationAdapter illustrationAdapter;

    /**
     * Create a new onboarding page with the desired string resources and icon adapter.
     *
     * @param titleResId a string resource id which will be shown in the onboarding page
     * @param messageResId a string resource id which will be shown in the onboarding page
     * @param illustrationAdapter an icon adapter for the onboarding page
     */
    public OnboardingPage(@StringRes final int titleResId, @StringRes final int messageResId, @Nullable final OnboardingIllustrationAdapter illustrationAdapter) {
        this.titleResId = titleResId;
        this.messageResId = messageResId;
        this.illustrationAdapter = illustrationAdapter;
    }

    /**
     * @return the string resource id of the title shown on the onboarding page
     */
    @StringRes
    public int getTitleResId() {
        return titleResId;
    }

    /**
     * @return the string resource id of the message shown on the onboarding page
     */
    @StringRes
    public int getMessageResId() {
        return messageResId;
    }

    /**
     * @return the icon adapter for the onboarding page
     */
    @Nullable
    public OnboardingIllustrationAdapter getIllustrationAdapter() {
        return illustrationAdapter;
    }

    /**
     * @param illustrationAdapter an icon adapter for the onboarding page
     */
    public void setIllustrationAdapter(OnboardingIllustrationAdapter illustrationAdapter) {
        this.illustrationAdapter = illustrationAdapter;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(titleResId);
        dest.writeInt(messageResId);
        dest.writeParcelable(illustrationAdapter, 0);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final Creator<OnboardingPage> CREATOR = new Creator<OnboardingPage>() {
        @Override
        public OnboardingPage createFromParcel(final Parcel in) {
            return new OnboardingPage(in);
        }

        @Override
        public OnboardingPage[] newArray(final int size) {
            return new OnboardingPage[size];
        }
    };

    private OnboardingPage(@NonNull final Parcel in) {
        titleResId = in.readInt();
        messageResId = in.readInt();
        illustrationAdapter = in.readParcelable(OnboardingIllustrationAdapter.class.getClassLoader());
    }
}
