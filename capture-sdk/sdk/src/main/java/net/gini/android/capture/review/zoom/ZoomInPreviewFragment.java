package net.gini.android.capture.review.zoom;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.review.RotatableTouchImageViewContainer;

public class ZoomInPreviewFragment extends Fragment {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";
    private ImageDocument mImageDocument;

    private RotatableTouchImageViewContainer mRotatableTouchImageViewContainer;

    public static ZoomInPreviewFragment newInstance(ImageDocument imageDocument) {
        Bundle args = new Bundle();
        args.putParcelable(ARGS_DOCUMENT, imageDocument);
        ZoomInPreviewFragment fragment = new ZoomInPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImageDocument = getArguments().getParcelable(ARGS_DOCUMENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_zoom_in_preview, container, false);
        setupInputHandlers(view);
        return view;
    }

    private void setupInputHandlers(View view) {
        view.findViewById(R.id.gc_action_close).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRotatableTouchImageViewContainer = view.findViewById(R.id.gc_rotatable_img_container);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getPhotoMemoryCache()
                    .get(context, mImageDocument, new AsyncCallback<Photo, Exception>() {
                        @Override
                        public void onSuccess(final Photo result) {

                            mRotatableTouchImageViewContainer.getImageView().setImageBitmap(
                                    result.getBitmapPreview());
                            mRotatableTouchImageViewContainer.rotateImageView(mImageDocument.getRotationForDisplay(), false);
                        }

                        @Override
                        public void onError(final Exception exception) {
                            //showPreviewError(context);
                        }

                        @Override
                        public void onCancelled() {
                            // Not used
                        }
                    });
        }
    }

}
