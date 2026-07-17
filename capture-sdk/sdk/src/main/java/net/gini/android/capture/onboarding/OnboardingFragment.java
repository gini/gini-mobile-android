package net.gini.android.capture.onboarding;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.ContextHelper;
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

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.GET_STARTED;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.NEXT;
import static net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton.SKIP;

/**
 * Internal use only.
 *
 * @suppress
 */
public class OnboardingFragment extends Fragment {

    private static final Logger LOG = LoggerFactory.getLogger(OnboardingFragment.class);

    private OnboardingViewModel mViewModel;

    private ViewPager mViewPager;
    private LinearLayout mLayoutPageIndicators;
    private PageIndicators mPageIndicators;
    private InjectedViewContainer<OnboardingNavigationBarBottomAdapter> injectedNavigationBarBottomContainer;
    private Button buttonNext;
    private Button buttonSkip;
    private Button buttonGetStarted;
    private Group groupNextAndSkipButtons;
    private ConstraintLayout bottomButtonsContainer;
    private OnboardingNavigationBarBottomButton[] navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{};

    /**
     * @param savedInstanceState
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        final ArrayList<OnboardingPage> customPages = getCustomOnboardingPages();
        if (customPages != null) {
            mViewModel.setCustomPages(customPages);
        }
    }

    @Nullable
    private ArrayList<OnboardingPage> getCustomOnboardingPages() {
        if (GiniCapture.hasInstance()) {
            return GiniCapture.getInstance().getCustomOnboardingPages();
        }
        return null;
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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
        mViewModel.start();
    }

    private void observeViewModel() {
        mViewModel.getPages().observe(getViewLifecycleOwner(), this::showPages);
        mViewModel.getScrollToPage().observe(getViewLifecycleOwner(), event -> {
            final Integer pageIndex = event.getContentIfNotHandled();
            if (pageIndex != null) {
                scrollToPage(pageIndex);
            }
        });
        mViewModel.getActivePageIndex().observe(getViewLifecycleOwner(),
                this::activatePageIndicatorForPage);
        mViewModel.getNavigationBarBottomAdapterInstance().observe(getViewLifecycleOwner(),
                adapterInstance -> {
                    setNavigationBarBottomAdapterInstance(adapterInstance);
                    hideButtons();
                });
        mViewModel.getButtonsState().observe(getViewLifecycleOwner(), this::updateButtons);
        mViewModel.getCloseOnboarding().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                close();
            }
        });
    }

    private void updateButtons(@NonNull final OnboardingButtonsState buttonsState) {
        switch (buttonsState) {
            case SKIP_AND_NEXT:
                showSkipAndNextButtons();
                break;
            case SKIP_AND_NEXT_IN_NAVIGATION_BAR_BOTTOM:
                showSkipAndNextButtonsInNavigationBarBottom();
                break;
            case GET_STARTED:
                showGetStartedButton();
                break;
            case GET_STARTED_IN_NAVIGATION_BAR_BOTTOM:
                showGetStartedButtonInNavigationBarBottom();
                break;
            default:
                break;
        }
    }

    private void hideButtons() {
        if (injectedNavigationBarBottomContainer != null) {
            groupNextAndSkipButtons.setVisibility(View.GONE);
            buttonGetStarted.setVisibility(View.GONE);
        }
    }

    private void close() {
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
        if (!ContextHelper.isPortraitOrTablet(requireContext())) {
            bottomButtonsContainer = view.findViewById(R.id.gc_bottom_container);
        }
        buttonSkip.setText(getString(R.string.gc_skip_two_lines));
        handleSkipButtonMultipleLines();
    }

    //Wait for view to be inflated
    //Check how many lines
    private void handleSkipButtonMultipleLines() {
        buttonSkip.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                int lines = ((Button)v).getLineCount();
                if (lines >= 2) {
                    ((Button)v).setText(getString(R.string.gc_skip_two_lines));
                }
            }
        });

    }

    private void addInputHandlers() {
        ClickListenerExtKt.setIntervalClickListener(buttonNext, v -> mViewModel.showNextPage());
        ClickListenerExtKt.setIntervalClickListener(buttonSkip, v -> mViewModel.skip());
        ClickListenerExtKt.setIntervalClickListener(buttonGetStarted, v -> mViewModel.showNextPage());
    }

    private void showPages(@NonNull List<OnboardingPage> pages) {
        setUpViewPager(pages);
    }

    private void setViewPagerAdapterForLandscape(@NonNull final List<OnboardingPage> pages) {
        handleViewPagerContentOnRunTime(pages);
    }
    private void setViewPagerAdapterForPortrait(@NonNull final List<OnboardingPage> pages) {
        final ViewPagerAdapterCompat viewPagerAdapter =
                new ViewPagerAdapterCompat(getChildFragmentManager(), pages, Integer.MIN_VALUE);
        clearViewPagerAdapter(viewPagerAdapter);
        mViewPager.setAdapter(viewPagerAdapter);
        viewPagerAdapter.notifyDataSetChanged();
    }

    private void clearViewPagerAdapter(ViewPagerAdapterCompat viewPagerAdapter) {
        requireActivity().runOnUiThread(() -> {
            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
            for (int i = 0; i < viewPagerAdapter.getCount(); i++) {
                String tag = "android:switcher:" + R.id.gc_onboarding_viewpager + ":" + i;
                Fragment fragment = fm.findFragmentByTag(tag);
                if (fragment != null) {
                    tx.remove(fragment);
                }
            }
            tx.commitNowAllowingStateLoss();
        });
    }

    private void setUpViewPager(@NonNull final List<OnboardingPage> pages) {

        if (!ContextHelper.isPortraitOrTablet(requireContext()) && bottomButtonsContainer != null)
            setViewPagerAdapterForLandscape(pages);
        else
            setViewPagerAdapterForPortrait(pages);

        mViewPager.setOffscreenPageLimit(1);
        final int numberOfPageIndicators = pages.size();
        mPageIndicators = new PageIndicators(getActivity(), numberOfPageIndicators, mLayoutPageIndicators);
        mPageIndicators.create();

        mViewPager.addOnPageChangeListener(new PageChangeListener(mViewModel));
    }

    private void handleViewPagerContentOnRunTime(@NonNull final List<OnboardingPage> pages) {
        bottomButtonsContainer.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        int safeThreshold = 15;
                        bottomButtonsContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                        final ViewPagerAdapterCompat viewPagerAdapter = new ViewPagerAdapterCompat(getChildFragmentManager(), pages,
                                bottomButtonsContainer.getHeight() + safeThreshold);
                        clearViewPagerAdapter(viewPagerAdapter);
                        mViewPager.setAdapter(viewPagerAdapter);
                        viewPagerAdapter.notifyDataSetChanged();
                        return true;
                    }
                });
    }
    private void scrollToPage(int pageIndex) {
        mViewPager.setCurrentItem(pageIndex);
    }

    private void activatePageIndicatorForPage(int pageIndex) {
        mPageIndicators.setActive(pageIndex);
    }

    private void showGetStartedButton() {
        groupNextAndSkipButtons.setVisibility(View.INVISIBLE);
        buttonGetStarted.setVisibility(View.VISIBLE);
    }

    private void showGetStartedButtonInNavigationBarBottom() {
        navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{GET_STARTED};
        if (injectedNavigationBarBottomContainer != null) {
            injectedNavigationBarBottomContainer.modifyAdapterIfOwned(adapter -> {
                adapter.showButtons(navigationBarBottomButtons);
                return Unit.INSTANCE;
            });
        }
    }

    private void showSkipAndNextButtons() {
        groupNextAndSkipButtons.setVisibility(View.VISIBLE);
        buttonGetStarted.setVisibility(View.INVISIBLE);
    }

    private void showSkipAndNextButtonsInNavigationBarBottom() {
        navigationBarBottomButtons = new OnboardingNavigationBarBottomButton[]{SKIP, NEXT};
        if (injectedNavigationBarBottomContainer != null) {
            injectedNavigationBarBottomContainer.modifyAdapterIfOwned(adapter -> {
                adapter.showButtons(navigationBarBottomButtons);
                return Unit.INSTANCE;
            });
        }
    }

    private void setNavigationBarBottomAdapterInstance(@NonNull InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> adapterInstance) {
        if (injectedNavigationBarBottomContainer != null) {
            injectedNavigationBarBottomContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(adapterInstance, injectedViewAdapter -> {
                injectedViewAdapter.setOnNextButtonClickListener(new IntervalClickListener(v -> mViewModel.showNextPage()));
                injectedViewAdapter.setOnSkipButtonClickListener(new IntervalClickListener(v -> mViewModel.skip()));
                injectedViewAdapter.setOnGetStartedButtonClickListener(new IntervalClickListener(v -> mViewModel.showNextPage()));
                injectedViewAdapter.showButtons(navigationBarBottomButtons);
            }));
        }
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
            pageIndicator.setImageAlpha(102);
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
                pageIndicator.setImageAlpha(102);
                pageIndicator.setContentDescription(mContext.getString(R.string.gc_onboarding_page_indicator_inactive_content_description, i + 1));
            }
        }

        List<ImageView> getPageIndicatorImageViews() {
            return mPageIndicators;
        }
    }

    private static class PageChangeListener implements ViewPager.OnPageChangeListener {

        private final OnboardingViewModel mViewModel;

        PageChangeListener(@NonNull final OnboardingViewModel viewModel) {
            mViewModel = viewModel;
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                                   final int positionOffsetPixels) {
            // No-op
        }

        @Override
        public void onPageSelected(final int position) {
            LOG.info("page selected: {}", position);
            mViewModel.onScrolledToPage(position);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            // No-op
        }
    }
}
