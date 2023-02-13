package net.gini.android.capture.onboarding;

import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.GET_STARTED;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.NEXT;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.SKIP;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import net.gini.android.capture.R;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.view.InjectedViewContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal use only.
 *
 * @suppress
 */
public class OnboardingFragment extends Fragment implements OnboardingScreenContract.View,
        OnboardingFragmentInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OnboardingFragment.class);

    private static final String ARGS_PAGES = "GC_PAGES";

    private OnboardingScreenContract.Presenter mPresenter;
    private OnboardingFragmentListener mListener;

    private ViewPager mViewPager;
    private LinearLayout mLayoutPageIndicators;
    private PageIndicators mPageIndicators;
    private InjectedViewContainer<OnboardingNavigationBarBottomAdapter> injectedNavigationBarBottomContainer;
    private Button buttonNext;
    private Button buttonSkip;
    private Button buttonGetStarted;
    private Group groupNextAndSkipButtons;

    /**
     * <p>
     * Factory method for creating a new instance of the Fragment using the provided list of
     * onboarding pages.
     * </p>
     * <p>
     * If you don't need a custom number of pages and wish to use the default behaviour, you can use
     * the default constructor of {@link OnboardingFragment}.
     * </p>
     *
     * @param pages the pages to be shown
     *
     * @return a new instance of the Fragment
     */
    public static OnboardingFragment createInstance(
            @NonNull final ArrayList<OnboardingPage> pages) { // NOPMD - ArrayList required (Bundle)
        final OnboardingFragment fragment = new OnboardingFragment();
        final Bundle arguments = new Bundle();
        arguments.putParcelableArrayList(ARGS_PAGES, pages);
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * @param savedInstanceState
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Missing activity for fragment.");
        }

        forcePortraitOrientationOnPhones(activity);

        initListener(activity);
        initPresenter(activity, getOnboardingPages());
    }

    private void initListener(@NonNull final Activity activity) {
        if (activity instanceof OnboardingFragmentListener) {
            setListener((OnboardingFragmentListener) activity);
        } else if (mListener == null) {
            throw new IllegalStateException(
                    "OnboardingFragmentListener not set. "
                            + "You can set it with OnboardingFragment#setListener() or "
                            + "by making the host activity implement the OnboardingFragmentListener.");
        }
    }

    @Nullable
    private ArrayList<OnboardingPage> getOnboardingPages() {
        final Bundle arguments = getArguments();
        ArrayList<OnboardingPage> pages = null;
        if (arguments != null) {
            pages = arguments.getParcelableArrayList(ARGS_PAGES);
        }
        return pages;
    }

    private void initPresenter(@NonNull final Activity activity, @Nullable final ArrayList<OnboardingPage> pages) { // NOPMD - Bundle
        createPresenter(activity);
        if (pages != null) {
            mPresenter.setCustomPages(pages);
        }
        mPresenter.setListener(mListener);
    }

    protected void createPresenter(@NonNull final Activity activity) {
        new OnboardingScreenPresenter(activity, this);
    }

    /**
     * @param inflater
     * @param container
     * @param savedInstanceState
     *
     * @return
     *
     * @suppress
     */
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_onboarding, container, false);
        bindViews(view);
        addInputHandlers();
        mPresenter.start();
        return view;
    }

    @Override
    public void hideButtons() {
        groupNextAndSkipButtons.setVisibility(View.GONE);
        buttonGetStarted.setVisibility(View.GONE);
    }

    private void bindViews(final View view) {
        mViewPager = (ViewPager) view.findViewById(R.id.gc_onboarding_viewpager);
        mLayoutPageIndicators = (LinearLayout) view.findViewById(R.id.gc_layout_page_indicators);
        injectedNavigationBarBottomContainer = view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        buttonNext = view.findViewById(R.id.gc_next);
        buttonSkip = view.findViewById(R.id.gc_skip);
        buttonGetStarted = view.findViewById(R.id.gc_get_started);
        groupNextAndSkipButtons = view.findViewById(R.id.gc_next_skip_group);

        handleSkipButtonMultipleLines();
    }

    //Wait for view to be inflated
    //Check how many lines
    private void handleSkipButtonMultipleLines() {
        buttonSkip.post(() -> {
            int lines = buttonSkip.getLineCount();
            if (lines != 2)
                return;

            buttonSkip.setText(getString(R.string.gc_skip_two_lines));
        });

    }

    private void addInputHandlers() {
        ClickListenerExtKt.setIntervalClickListener(buttonNext, v -> mPresenter.showNextPage());
        ClickListenerExtKt.setIntervalClickListener(buttonSkip, v -> mPresenter.skip());
        ClickListenerExtKt.setIntervalClickListener(buttonGetStarted, v -> mPresenter.showNextPage());
    }

    @Override
    public void setListener(@NonNull final OnboardingFragmentListener listener) {
        mListener = listener;
        if (mPresenter != null) {
            mPresenter.setListener(listener);
        }
    }

    @Override
    public void setPresenter(@NonNull OnboardingScreenContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showPages(@NonNull List<OnboardingPage> pages) {
        setUpViewPager(pages);
    }

    private void setUpViewPager(@NonNull final List<OnboardingPage> pages) {
        final ViewPagerAdapterCompat viewPagerAdapter = new ViewPagerAdapterCompat(getChildFragmentManager(), pages);
        mViewPager.setAdapter(viewPagerAdapter);

        final int numberOfPageIndicators = pages.size();
        mPageIndicators = new PageIndicators(getActivity(), numberOfPageIndicators, mLayoutPageIndicators);
        mPageIndicators.create();

        mViewPager.addOnPageChangeListener(new PageChangeListener(mPresenter));
    }

    @Override
    public void scrollToPage(int pageIndex) {
        mViewPager.setCurrentItem(pageIndex);
    }

    @Override
    public void activatePageIndicatorForPage(int pageIndex) {
        mPageIndicators.setActive(pageIndex);
    }

    @Override
    public void showGetStartedButton() {
        groupNextAndSkipButtons.setVisibility(View.INVISIBLE);
        buttonGetStarted.setVisibility(View.VISIBLE);
    }

    @Override
    public void showGetStartedButtonInNavigationBarBottom() {
        final OnboardingNavigationBarBottomAdapter injectedViewAdapter = injectedNavigationBarBottomContainer.getInjectedViewAdapter();
        if (injectedViewAdapter != null) {
            injectedViewAdapter.showButtons(GET_STARTED);
        }
    }

    @Override
    public void showSkipAndNextButtons() {
        groupNextAndSkipButtons.setVisibility(View.VISIBLE);
        buttonGetStarted.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showSkipAndNextButtonsInNavigationBarBottom() {
        final OnboardingNavigationBarBottomAdapter injectedViewAdapter = injectedNavigationBarBottomContainer.getInjectedViewAdapter();
        if (injectedViewAdapter != null) {
            injectedViewAdapter.showButtons(SKIP, NEXT);
        }
    }

    @Override
    public void setNavigationBarBottomAdapter(@NonNull OnboardingNavigationBarBottomAdapter adapter) {
        injectedNavigationBarBottomContainer.setInjectedViewAdapter(adapter); // view.setNavigationBarBottomAdapter()

        adapter.setOnNextButtonClickListener(new IntervalClickListener(v -> mPresenter.showNextPage()));
        adapter.setOnSkipButtonClickListener(new IntervalClickListener(v -> mPresenter.skip()));
        adapter.setOnGetStartedButtonClickListener(new IntervalClickListener(v -> mPresenter.showNextPage()));
    }

    static class PageIndicators {

        private final Context mContext;
        private final int mNrOfPages;
        private final LinearLayout mLayoutPageIndicators;
        private final List<ImageView> mPageIndicators = new ArrayList<>();

        PageIndicators(final Context context, final int nrOfPages,
                       final LinearLayout layoutPageIndicators) {
            mContext = context;
            mNrOfPages = nrOfPages;
            mLayoutPageIndicators = layoutPageIndicators;
        }

        public void create() {
            createPageIndicators(mNrOfPages);
            for (int i = 0; i < mPageIndicators.size(); i++) {
                final ImageView pageIndicator = mPageIndicators.get(i);
                mLayoutPageIndicators.addView(pageIndicator);
                if (i < mPageIndicators.size() - 1) {
                    mLayoutPageIndicators.addView(createSpace());
                }
            }
        }

        private void createPageIndicators(final int nrOfPages) {
            for (int i = 0; i < nrOfPages; i++) {
                mPageIndicators.add(createPageIndicator());
            }
        }

        private ImageView createPageIndicator() {
            final ImageView pageIndicator = new ImageView(mContext);
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.gc_onboarding_indicator_width),
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.gc_onboarding_indicator_height));
            pageIndicator.setLayoutParams(layoutParams);
            pageIndicator.setScaleType(ImageView.ScaleType.CENTER);
            pageIndicator.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.getResources(),
                            R.drawable.gc_onboarding_page_indicator, mContext.getTheme()));
            pageIndicator.setImageAlpha(77);
            pageIndicator.setTag("pageIndicator");
            return pageIndicator;
        }

        private Space createSpace() {
            final Space space = new Space(mContext);
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.gc_onboarding_indicator_width),
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.gc_onboarding_indicator_height));
            space.setLayoutParams(layoutParams);
            return space;
        }

        public void setActive(final int page) {
            if (page >= mPageIndicators.size()) {
                return;
            }
            deactivatePageIndicators();
            final ImageView pageIndicator = mPageIndicators.get(page);
            pageIndicator.setImageAlpha(255);
            pageIndicator.setContentDescription(mContext.getString(R.string.gc_onboarding_page_indicator_active_content_description, page + 1));
        }

        private void deactivatePageIndicators() {
            for (int i = 0; i < mPageIndicators.size(); i++) {
                final ImageView pageIndicator = mPageIndicators.get(i);
                pageIndicator.setImageAlpha(77);
                pageIndicator.setContentDescription(mContext.getString(R.string.gc_onboarding_page_indicator_inactive_content_description, i + 1));
            }
        }

        List<ImageView> getPageIndicatorImageViews() {
            return mPageIndicators;
        }
    }

    private static class PageChangeListener implements ViewPager.OnPageChangeListener {

        private final OnboardingScreenContract.Presenter mPresenter;

        PageChangeListener(@NonNull final OnboardingScreenContract.Presenter presenter) {
            mPresenter = presenter;
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                                   final int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(final int position) {
            LOG.info("page selected: {}", position);
            mPresenter.onScrolledToPage(position);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
        }
    }
}
