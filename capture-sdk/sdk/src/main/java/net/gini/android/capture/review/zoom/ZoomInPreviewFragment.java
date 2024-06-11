package net.gini.android.capture.review.zoom;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.review.RotatableTouchImageViewContainer;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;

import java.util.HashSet;

public class ZoomInPreviewFragment extends Fragment {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";
    private ImageDocument mImageDocument;

    private RotatableTouchImageViewContainer mRotatableTouchImageViewContainer;
    private final UserAnalyticsScreen screenName = UserAnalyticsScreen.ReviewZoom.INSTANCE;

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

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        return getLayoutInflaterWithGiniCaptureTheme(this, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_zoom_in_preview, container, false);
        setupInputHandlers(view);
        UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(UserAnalyticsEvent.SCREEN_SHOWN,
                new HashSet<UserAnalyticsEventProperty>() {
                    {
                        add(new UserAnalyticsEventProperty.Screen(screenName));
                    }
                });
        return view;
    }

    private void setupInputHandlers(View view) {
        view.findViewById(R.id.gc_action_close).setOnClickListener(v -> {
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    });
            NavHostFragment.findNavController(this).popBackStack();
        });
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                            }
                        });
                remove();
                NavHostFragment.findNavController(ZoomInPreviewFragment.this).popBackStack();
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
