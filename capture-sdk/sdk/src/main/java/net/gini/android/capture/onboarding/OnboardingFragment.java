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
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.view.InjectedViewContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    }

    private void addInputHandlers() {
        buttonNext.setOnClickListener(v -> mPresenter.showNextPage());
        buttonSkip.setOnClickListener(v -> mPresenter.skip());
        buttonGetStarted.setOnClickListener(v -> mPresenter.showNextPage());
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

        adapter.setOnNextButtonClickListener(v -> mPresenter.showNextPage());
        adapter.setOnSkipButtonClickListener(v -> mPresenter.skip());
        adapter.setOnGetStartedButtonClickListener(v -> mPresenter.showNextPage());
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
