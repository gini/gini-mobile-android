package net.gini.android.capture.onboarding;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import net.gini.android.capture.R;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewAdapterInstance;
import net.gini.android.capture.view.InjectedViewContainer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Internal use only.
 *
 * @suppress
 */
public class OnboardingPageFragment extends Fragment implements OnboardingPageContract.View {

    private static final String ARGS_PAGE = "GC_PAGE";
    private static final String ARGS_PADDING_FOR_LANDSCAPE = "ARGS_PADDING_FOR_LANDSCAPE";
    private static final String ARGS_IS_LAST_PAGE = "GC_IS_LAST_PAGE";

    private OnboardingPageContract.Presenter mPresenter;

    private boolean isLastPage;

    private TextView mTextTitle;
    private TextView mTextMessage;
    private InjectedViewContainer<OnboardingIllustrationAdapter> injectedIconContainer;

    public static OnboardingPageFragment createInstance(@NonNull final OnboardingPage page,
                                                        final boolean isLastPage,
                                                        final int paddingForLandscape) {
        final OnboardingPageFragment fragment = new OnboardingPageFragment();
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_PAGE, page);
        arguments.putInt(ARGS_PADDING_FOR_LANDSCAPE, paddingForLandscape);
        arguments.putBoolean(ARGS_IS_LAST_PAGE, isLastPage);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Missing activity for fragment.");
        }

        isLastPage = getIsLastPage();

        initPresenter(activity, getOnboardingPage());
    }

    private boolean getIsLastPage() {
        final Bundle arguments = getArguments();
        if (arguments != null) {
            return arguments.getBoolean(ARGS_IS_LAST_PAGE);
        }
        return false;
    }

    private int getPadding() {
        final Bundle arguments = getArguments();
        if (arguments != null) {
            return arguments.getInt(ARGS_PADDING_FOR_LANDSCAPE);
        }
        return Integer.MIN_VALUE;
    }
    @NonNull
    private OnboardingPage getOnboardingPage() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("Missing OnboardingPage.");
        }

        final OnboardingPage page = arguments.getParcelable(ARGS_PAGE);
        if (page == null) {
            throw new IllegalStateException("Missing OnboardingPage.");
        }

        return page;
    }

    private void initPresenter(@NonNull final Activity activity,
                               @NonNull final OnboardingPage page) {
        createPresenter(activity);
        mPresenter.setPage(page);
    }

    private void createPresenter(@NonNull final Activity activity) {
        new OnboardingPagePresenter(activity, this);
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        return getLayoutInflaterWithGiniCaptureTheme(this, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_onboarding_page, container, false);
        bindViews(view);
        mPresenter.start();
        return view;
    }

    private void adjustPaddingsLandscape(@NonNull final View view) {
        int bottomPadding = getPadding();
        if (!ContextHelper.isPortraitOrTablet(requireContext()) && bottomPadding != Integer.MIN_VALUE) {
            ScrollView mScrollContainer = view.findViewById(R.id.gc_scroll_container);
            mScrollContainer.setPadding(
                    mScrollContainer.getPaddingLeft(),
                    mScrollContainer.getPaddingTop(),
                    mScrollContainer.getPaddingRight(),
                    bottomPadding
            );
        }
    }
    private void bindViews(@NonNull final View view) {
        mTextTitle = view.findViewById(R.id.gc_title);
        mTextMessage = view.findViewById(R.id.gc_message);
        adjustPaddingsLandscape(view);
        injectedIconContainer = view.findViewById(R.id.gc_injected_icon_container);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.onPageIsVisible();
    }

    @Override
    public void setPresenter(@NonNull OnboardingPageContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showImage(@NonNull OnboardingIllustrationAdapter illustrationAdapter) {
        injectedIconContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                // We can create our local instance because onboarding illustrations are not shown on multiple screens
                new InjectedViewAdapterInstance<>(illustrationAdapter), injectedViewAdapter -> {
        }));
    }

    @Override
    public void showTitle(int titleResId) {
        mTextTitle.setText(titleResId);
    }

    @Override
    public void showMessage(int messageResId) {
        mTextMessage.setText(messageResId);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.onPageIsHidden();
    }
}
