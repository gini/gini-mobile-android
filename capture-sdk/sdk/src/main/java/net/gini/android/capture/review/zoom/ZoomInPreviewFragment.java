package net.gini.android.capture.review.zoom;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.R;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.review.RotatableTouchImageViewContainer;

public class ZoomInPreviewFragment extends Fragment {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";
    private ImageDocument mImageDocument;

    private RotatableTouchImageViewContainer mRotatableTouchImageViewContainer;
    private ZoomInPreviewViewModel mViewModel;

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
        mViewModel = new ViewModelProvider(this,
                new ZoomInPreviewViewModel.Factory(requireActivity().getApplication(),
                        mImageDocument))
                .get(ZoomInPreviewViewModel.class);
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
        mViewModel.onScreenShown();
        return view;
    }

    private void setupInputHandlers(View view) {
        view.findViewById(R.id.gc_action_close).setOnClickListener(v ->
                mViewModel.onCloseClicked());
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                remove();
                mViewModel.onCloseClicked();
            }
        });
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRotatableTouchImageViewContainer = view.findViewById(R.id.gc_rotatable_img_container);
        observeViewModel();
    }

    private void observeViewModel() {
        mViewModel.getPreview().observe(getViewLifecycleOwner(), uiState -> {
            mRotatableTouchImageViewContainer.getImageView().setImageBitmap(
                    uiState.getPreviewBitmap());
            mRotatableTouchImageViewContainer.rotateImageView(uiState.getRotationForDisplay(),
                    false);
        });
        mViewModel.getCloseEvent().observe(getViewLifecycleOwner(), consumableEvent -> {
            if (consumableEvent.getContentIfNotHandled() != null) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mViewModel.onStart();
    }

}
