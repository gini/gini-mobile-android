package net.gini.android.capture.onboarding;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavigationBarBottomAdapter;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * <h3>Component API</h3>
 *
 * <p>
 * When you use the Component API with the Android Support Library, the {@code
 * OnboardingFragmentCompat} displays important advice for correctly photographing a document.
 * </p>
 * <p>
 * <b>Note:</b> Your Activity hosting this Fragment must extend the {@link
 * androidx.appcompat.app.AppCompatActivity} and use an AppCompat Theme.
 * </p>
 * <p>
 * Include the {@code OnboardingFragmentCompat} into your layout either directly with {@code
 * <fragment>} in your Activity's layout or using the {@link androidx.fragment.app.FragmentManager}.
 * </p>
 * <p>
 * The default way of showing the Onboarding Screen is as an overlay above the camera preview with a
 * semi-transparent background.
 * </p>
 * <p>
 * By default an empty last page is added to enable the revealing of the camera preview before the
 * Onboarding Screen is dismissed.
 * </p>
 * <p>
 * If you would like to display a different number of pages, you can use the {@link
 * OnboardingFragment#createInstance(ArrayList)} or {@link OnboardingFragment#createInstanceWithoutEmptyLastPage(ArrayList)}
 * factory method and provide a list of {@link OnboardingPage} objects.
 * </p>
 * <p>
 * If you would like to disable the appending of the empty last page, you can use the {@link
 * OnboardingFragment#createInstanceWithoutEmptyLastPage(ArrayList)} or the {@link
 * OnboardingFragment#createInstanceWithoutEmptyLastPage()} factory method.
 * </p>
 * <p>
 * An {@link OnboardingFragmentListener} instance must be available until the {@code
 * OnboardingFragmentCompat} is attached to an activity. Failing to do so will throw an exception.
 * The listener instance can be provided either implicitly by making the hosting Activity implement
 * the {@link OnboardingFragmentListener} interface or explicitly by setting the listener using
 * {@link OnboardingFragment#setListener(OnboardingFragmentListener)}.
 * </p>
 * <p>
 * Your Activity is automatically set as the listener in {@link OnboardingFragment#onCreate(Bundle)}.
 * </p>
 *
 * <h3>Customizing the Onboarding Screen</h3>
 *
 * <p>
 * See the {@link OnboardingActivity} for details.
 * </p>
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
    private InjectedViewContainer injectedNavigationBarTopContainer;
    private InjectedViewContainer injectedNavigationBarBottomContainer;

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

    private void createPresenter(@NonNull final Activity activity) {
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
        //<editor-fold desc="Navigation bar injection experiments">

        final Activity activity = getActivity();

        if (GiniCapture.hasInstance()) {
            NavigationBarTopAdapter navigationBarTopAdapter = GiniCapture.getInstance().getNavigationBarTopAdapter();
            injectedNavigationBarTopContainer.setInjectedViewAdapter(navigationBarTopAdapter);

            navigationBarTopAdapter.setOnNavButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Activity activity = getActivity();
                    if (activity != null) {
                        activity.onBackPressed();
                    }
                }
            });

            if (activity != null) {
                navigationBarTopAdapter.setTitle(activity.getTitle().toString());
            }

            if (GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
                NavigationBarBottomAdapter navigationBarBottomAdapter = GiniCapture.getInstance().getNavigationBarBottomAdapter();
                injectedNavigationBarBottomContainer.setInjectedViewAdapter(navigationBarBottomAdapter);

                navigationBarBottomAdapter.setOnBackButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            activity.onBackPressed();
                        }
                    }
                });
            }
        }
        //</editor-fold>
        mPresenter.start();
        return view;
    }

    private void bindViews(final View view) {
        mViewPager = (ViewPager) view.findViewById(R.id.gc_onboarding_viewpager);
        mLayoutPageIndicators = (LinearLayout) view.findViewById(R.id.gc_layout_page_indicators);
        injectedNavigationBarTopContainer = view.findViewById(R.id.gc_injected_navigation_bar_container_top);
        injectedNavigationBarBottomContainer = view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);
    }

    @Override
    public void setListener(@NonNull final OnboardingFragmentListener listener) {
        mListener = listener;
        if (mPresenter != null) {
            mPresenter.setListener(listener);
        }
    }

    @Override
    public void showNextPage() {
        mPresenter.showNextPage();
    }

    @Override
    public void skip() {
        mPresenter.skip();
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
    public CompletableFuture<Void> slideOutViews() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        // If width is still 0  set it to a big value to make sure the view
        // will slide out completely
        int layoutPageIndicatorsWidth = mLayoutPageIndicators.getWidth();
        layoutPageIndicatorsWidth =
                layoutPageIndicatorsWidth != 0 ? layoutPageIndicatorsWidth : 10000;

        mLayoutPageIndicators.animate()
                .setDuration(150)
                .translationX(-10 * layoutPageIndicatorsWidth);
        return future;
    }

    private static class PageIndicators {

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
            pageIndicator.setContentDescription("inactive");
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
            pageIndicator.setContentDescription("active");
        }

        private void deactivatePageIndicators() {
            for (final ImageView pageIndicator : mPageIndicators) {
                pageIndicator.setImageAlpha(77);
                pageIndicator.setContentDescription("inactive");
            }
        }

        private List<ImageView> getPageIndicatorImageViews() {
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
