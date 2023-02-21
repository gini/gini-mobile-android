package net.gini.android.capture.noresults;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.ImageRetakeOptionsListener;
import net.gini.android.capture.R;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.help.PhotoTipsAdapter;
import net.gini.android.capture.help.SupportedFormatsAdapter;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.GONE;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

/**
 * Main logic implementation for no results UI presented by {@link NoResultsActivity}.
 * Internal use only.
 */
class NoResultsFragmentImpl {

    private static final ImageRetakeOptionsListener NO_OP_LISTENER =
            new ImageRetakeOptionsListener() {
                @Override
                public void onBackToCameraPressed() {}

                @Override
                public void onEnterManuallyPressed() {}
            };

    private final FragmentImplCallback mFragment;
    private final Document mDocument;
    private ImageRetakeOptionsListener mListener;

    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;

    NoResultsFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Document document) {
        mFragment = fragment;
        mDocument = document;
    }

    void setListener(@Nullable final ImageRetakeOptionsListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    void onCreate(final Bundle savedInstanceState) {
        forcePortraitOrientationOnPhones(mFragment.getActivity());
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_noresults, container, false);
        final View retakeImagesButton = view.findViewById(R.id.gc_button_no_results_retake_images);
        if (shouldAllowRetakeImages()) {
            ClickListenerExtKt.setIntervalClickListener(retakeImagesButton, v -> {
                mListener.onBackToCameraPressed();
            });
        } else {
            retakeImagesButton.setVisibility(GONE);
        }


        final View enterManuallyButton = view.findViewById(R.id.gc_button_no_results_enter_manually);
        ClickListenerExtKt.setIntervalClickListener(enterManuallyButton, v -> {
            mListener.onEnterManuallyPressed();
        });

        bindViews(view);
        setTopBarInjectedViewContainer();
        setUpList(view);

        return view;
    }

    private boolean isDocumentFromCameraScreen(Document document) {
        return document.getImportMethod() != Document.ImportMethod.OPEN_WITH && document.getSource().getName().equals("camera");
    }

    private boolean shouldAllowRetakeImages() {
        if (mDocument instanceof ImageMultiPageDocument) {
            ImageMultiPageDocument doc = (ImageMultiPageDocument) mDocument;
            boolean isImportedDocFound = false;
            int i = 0;

            while (!isImportedDocFound && i < doc.getDocuments().size()) {
                isImportedDocFound = !isDocumentFromCameraScreen(doc.getDocuments().get(i));
                i++;
            }

            return !isImportedDocFound;
        }

        return isDocumentFromCameraScreen(mDocument);
    }

    private void setUpList(View view) {
        final RecyclerView recyclerView = view.findViewById(R.id.gc_no_results_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragment.getActivity()));
        if (mDocument.getType() == Document.Type.PDF || mDocument.getType() == Document.Type.PDF_MULTI_PAGE) {
            recyclerView.setAdapter(new SupportedFormatsAdapter());
            return;
        }
        recyclerView.setAdapter(new PhotoTipsAdapter(view.getContext(), true));
    }

    private void setTopBarInjectedViewContainer() {
        if (GiniCapture.hasInstance()) {
            topAdapterInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            if (topAdapterInjectedViewContainer.getInjectedViewAdapter() == null)
                return;

            if (mFragment.getActivity() == null)
                return;

            topAdapterInjectedViewContainer.getInjectedViewAdapter().setTitle(mFragment.getActivity().getResources().getString(R.string.gc_title_no_results));

            if (GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
                return;
            }

            topAdapterInjectedViewContainer.getInjectedViewAdapter().setNavButtonType(NavButtonType.BACK);
            topAdapterInjectedViewContainer.getInjectedViewAdapter().setOnNavButtonClickListener(new IntervalClickListener(view -> mFragment.getActivity().onBackPressed()));
        }
    }

    private void bindViews(@NonNull final View view) {
        topAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
    }
}
