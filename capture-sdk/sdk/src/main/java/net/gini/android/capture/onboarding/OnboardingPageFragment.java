package net.gini.android.capture.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.gini.android.capture.R;
import net.gini.android.capture.onboarding.view.OnboardingIconAdapter;
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
    private static final String ARGS_IS_LAST_PAGE = "GC_IS_LAST_PAGE";

    private OnboardingPageContract.Presenter mPresenter;

    private boolean isLastPage;

    private TextView mTextTitle;
    private TextView mTextMessage;
    private InjectedViewContainer injectedIconContainer;
    private Button mButtonNext;
    private Button mButtonSkip;
    private Button mButtonGetStarted;

    public static OnboardingPageFragment createInstance(@NonNull final OnboardingPage page, final boolean isLastPage) {
        final OnboardingPageFragment fragment = new OnboardingPageFragment();
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_PAGE, page);
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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_onboarding_page, container, false);
        bindViews(view);
        setupViews(view);
        addInputHandlers();
        mPresenter.start();
        return view;
    }

    private void setupViews(View view) {
        final View group = view.findViewById(R.id.gc_next_skip_group);
        group.setVisibility(isLastPage ? View.INVISIBLE : View.VISIBLE);
        mButtonGetStarted.setVisibility(isLastPage ? View.VISIBLE : View.INVISIBLE);
    }

    private void bindViews(@NonNull final View view) {
        mTextTitle = view.findViewById(R.id.gc_title);
        mTextMessage = view.findViewById(R.id.gc_message);
        injectedIconContainer = view.findViewById(R.id.gc_injected_icon_container);
        mButtonNext = view.findViewById(R.id.gc_next);
        mButtonSkip = view.findViewById(R.id.gc_skip);
        mButtonGetStarted = view.findViewById(R.id.gc_get_started);
    }

    private void addInputHandlers() {
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragment() instanceof OnboardingFragmentInterface) {
                    ((OnboardingFragmentInterface) getParentFragment()).showNextPage();
                }
            }
        });
        mButtonSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragment() instanceof OnboardingFragmentInterface) {
                    ((OnboardingFragmentInterface) getParentFragment()).skip();
                }
            }
        });
        mButtonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragment() instanceof OnboardingFragmentInterface) {
                    ((OnboardingFragmentInterface) getParentFragment()).showNextPage();
                }
            }
        });
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
    public void showImage(@NonNull OnboardingIconAdapter iconAdapter) {
        injectedIconContainer.setInjectedViewAdapter(iconAdapter);
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
