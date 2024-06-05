package net.gini.android.capture.onboarding;

import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewAdapterInstance;
import net.gini.android.capture.view.InjectedViewContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

/**
 * Internal use only.
 *
 * @suppress
 */
public class OnboardingFragment extends Fragment implements OnboardingScreenContract.View {

    private static final Logger LOG = LoggerFactory.getLogger(OnboardingFragment.class);

    private OnboardingScreenContract.Presenter mPresenter;

    private ViewPager mViewPager;
    private LinearLayout mLayoutPageIndicators;
    private PageIndicators mPageIndicators;
    private InjectedViewContainer<OnboardingNavigationBarBottomAdapter> injectedNavigationBarBottomContainer;
    private Button buttonNext;
    private Button buttonSkip;
    private Button buttonGetStarted;
    private Group groupNextAndSkipButtons;
    private OnboardingNavigationBarBottomButton[] navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{};

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

        initPresenter(activity, getCustomOnboardingPages());
    }

    @Nullable
    private ArrayList<OnboardingPage> getCustomOnboardingPages() {
        if (GiniCapture.hasInstance()) {
            return GiniCapture.getInstance().getCustomOnboardingPages();
        }
        return null;
    }

    private void initPresenter(@NonNull final Activity activity, @Nullable final ArrayList<OnboardingPage> pages) { // NOPMD - Bundle
        createPresenter(activity);
        if (pages != null) {
            mPresenter.setCustomPages(pages);
        }
    }

    protected void createPresenter(@NonNull final Activity activity) {
        new OnboardingScreenPresenter(activity, this);
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

    @Override
    public void close() {
        NavHostFragment.findNavController(this).popBackStack();
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
        navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{GET_STARTED};
        injectedNavigationBarBottomContainer.modifyAdapterIfOwned(adapter -> {
            adapter.showButtons(navigationBarBottomButtons);
            return Unit.INSTANCE;
        });
    }

    @Override
    public void showSkipAndNextButtons() {
        groupNextAndSkipButtons.setVisibility(View.VISIBLE);
        buttonGetStarted.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showSkipAndNextButtonsInNavigationBarBottom() {
        navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{SKIP, NEXT};
        injectedNavigationBarBottomContainer.modifyAdapterIfOwned(adapter -> {
            adapter.showButtons(navigationBarBottomButtons);
            return Unit.INSTANCE;
        });
    }

    @Override
    public void setNavigationBarBottomAdapterInstance(@NonNull InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> adapterInstance) {
        injectedNavigationBarBottomContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(adapterInstance, injectedViewAdapter -> {
            injectedViewAdapter.setOnNextButtonClickListener(new IntervalClickListener(v -> mPresenter.showNextPage()));
            injectedViewAdapter.setOnSkipButtonClickListener(new IntervalClickListener(v -> mPresenter.skip()));
            injectedViewAdapter.setOnGetStartedButtonClickListener(new IntervalClickListener(v -> mPresenter.showNextPage()));
            injectedViewAdapter.showButtons(navigationBarBottomButtons);
        }));
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
