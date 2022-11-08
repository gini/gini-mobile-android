package net.gini.android.capture.noresults;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gini.android.capture.Document;
import net.gini.android.capture.R;
import net.gini.android.capture.help.PhotoTipsAdapter;
import net.gini.android.capture.help.SupportedFormatsAdapter;
import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.GONE;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

class NoResultsFragmentImpl {

    private static final NoResultsFragmentListener NO_OP_LISTENER =
            new NoResultsFragmentListener() {
                @Override
                public void onBackToCameraPressed() {
                }
            };

    private final FragmentImplCallback mFragment;
    private final Document mDocument;
    private NoResultsFragmentListener mListener;

    NoResultsFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Document document) {
        mFragment = fragment;
        mDocument = document;
    }

    void setListener(@Nullable final NoResultsFragmentListener listener) {
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
        if (isDocumentFromCameraScreen()) {
            retakeImagesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    mListener.onBackToCameraPressed();
                }
            });
        } else {
            retakeImagesButton.setVisibility(GONE);
        }

        setUpList(view);

        return view;
    }

    private boolean isDocumentFromCameraScreen() {
        return mDocument.getImportMethod() != Document.ImportMethod.OPEN_WITH && mDocument.getSource().getName().equals("camera");
    }

    private void setUpList(View view) {
        final RecyclerView recyclerView = view.findViewById(R.id.gc_no_results_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragment.getActivity()));

        if (mDocument.getType() == Document.Type.PDF || mDocument.getType() == Document.Type.PDF_MULTI_PAGE) {
            recyclerView.setAdapter(new SupportedFormatsAdapter());

            return;
        }

        recyclerView.setAdapter(new PhotoTipsAdapter(view.getContext()));
    }
}
