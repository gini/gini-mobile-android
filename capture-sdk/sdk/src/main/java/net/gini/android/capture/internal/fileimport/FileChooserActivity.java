package net.gini.android.capture.internal.fileimport;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RelativeLayout;

import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAdapter;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppItem;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersItem;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSectionItem;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSeparatorItem;
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSpanSizeLookup;
import net.gini.android.capture.internal.util.MimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionManager;

import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_PICK;
import static net.gini.android.capture.GiniCaptureError.ErrorCode.DOCUMENT_IMPORT;
import static net.gini.android.capture.internal.util.ContextHelper.isTablet;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isMultiPageEnabled;

/**
 * Internal use only.
 *
 * @suppress
 */
public class FileChooserActivity extends AppCompatActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FileChooserActivity.class);

    private static final int REQ_CODE_CHOOSE_FILE = 1;

    public static final String EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES =
            "GC_EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES";

    public static final int RESULT_ERROR = RESULT_FIRST_USER + 1;
    public static final String EXTRA_OUT_ERROR = "GC_EXTRA_OUT_ERROR";

    public static final int GRID_SPAN_COUNT_PHONE = 3;
    public static final int GRID_SPAN_COUNT_TABLET = 6;

    private static final int ANIM_DURATION = 200;
    private static final int SHOW_ANIM_DELAY = 300;

    private RelativeLayout mLayoutRoot;
    private RecyclerView mFileProvidersView;
    private DocumentImportEnabledFileTypes mDocImportEnabledFileTypes =
            DocumentImportEnabledFileTypes.NONE;


    public static boolean canChooseFiles(@NonNull final Context context) {
        final List<ResolveInfo> imagePickerResolveInfos = queryImagePickers(context);
        final List<ResolveInfo> imageProviderResolveInfos = queryImageProviders(context);
        final List<ResolveInfo> pdfProviderResolveInfos = queryPdfProviders(context);

        return !imagePickerResolveInfos.isEmpty()
                || !imageProviderResolveInfos.isEmpty()
                || !pdfProviderResolveInfos.isEmpty();
    }

    public static Intent createIntent(final Context context) {
        return new Intent(context, FileChooserActivity.class);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_file_chooser);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }
        bindViews();
        setInputHandlers();
        readExtras();
        setupFileProvidersView();
        overridePendingTransition(0, 0);
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mFileProvidersView.getTag() == null) {
                    return;
                }
                final boolean isShown = (boolean) mFileProvidersView.getTag();
                if (!isShown) {
                    return;
                }
                overridePendingTransition(0, 0);
                hideFileProviders(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(@NonNull final Transition transition) {
                        setEnabled(false);
                        onBackPressed();
                    }
                });
            }
        });
    }


    private void bindViews() {
        mLayoutRoot = findViewById(R.id.gc_layout_root);
        mFileProvidersView = findViewById(R.id.gc_file_providers);
    }

    private void setInputHandlers() {
        mLayoutRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mFileProvidersView == null) {
                    return;
                }
                final Object isShown = mFileProvidersView.getTag();
                if (isShown != null && (boolean) isShown) {
                    hideFileProviders(new TransitionListenerAdapter() {
                        @Override
                        public void onTransitionEnd(@NonNull final Transition transition) {
                            finish();
                        }
                    });
                }
            }
        });
    }

    private void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final DocumentImportEnabledFileTypes enabledFileTypes =
                    (DocumentImportEnabledFileTypes) extras.getSerializable(
                            EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES);
            if (enabledFileTypes != null) {
                mDocImportEnabledFileTypes = enabledFileTypes;
            }
        }
    }

    private void setupFileProvidersView() {
        mFileProvidersView.setLayoutManager(new GridLayoutManager(this, getGridSpanCount()));
    }

    private int getGridSpanCount() {
        return isTablet(this) ? GRID_SPAN_COUNT_TABLET : GRID_SPAN_COUNT_PHONE;
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFileProviders();
        showFileProviders();
    }

    private void showFileProviders() {
        mLayoutRoot.postDelayed(new Runnable() {
            @Override
            public void run() {
                final AutoTransition transition = new AutoTransition();
                transition.setDuration(ANIM_DURATION);
                TransitionManager.beginDelayedTransition(mLayoutRoot, transition);
                final RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) mFileProvidersView.getLayoutParams();
                layoutParams.addRule(RelativeLayout.BELOW);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                mFileProvidersView.setLayoutParams(layoutParams);
                mFileProvidersView.setTag(true);
            }
        }, SHOW_ANIM_DELAY);
    }

    private void hideFileProviders(
            @NonNull final Transition.TransitionListener transitionListener) {
        final AutoTransition transition = new AutoTransition();
        transition.setDuration(ANIM_DURATION);
        transition.addListener(transitionListener);
        TransitionManager.beginDelayedTransition(mLayoutRoot, transition);
        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mFileProvidersView.getLayoutParams();
        layoutParams.addRule(RelativeLayout.BELOW, R.id.gc_space);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mFileProvidersView.setLayoutParams(layoutParams);
        mFileProvidersView.setTag(false);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_CHOOSE_FILE) {
            setResult(resultCode, data);
        } else {
            final GiniCaptureError error = new GiniCaptureError(DOCUMENT_IMPORT,
                    "Unexpected request code for activity result.");
            final Intent result = new Intent();
            result.putExtra(EXTRA_OUT_ERROR, error);
            setResult(RESULT_ERROR, result);
        }
        finish();
    }

    private void populateFileProviders() {
        final List<ProvidersItem> providerItems = new ArrayList<>();
        List<ProvidersItem> imageProviderItems = new ArrayList<>();
        List<ProvidersItem> pdfProviderItems = new ArrayList<>();
        if (shouldShowImageProviders()) {
            final List<ResolveInfo> imagePickerResolveInfos = queryImagePickers(this);
            final List<ResolveInfo> imageProviderResolveInfos = queryImageProviders(this);
            imageProviderItems = getImageProviderItems(imagePickerResolveInfos,
                    imageProviderResolveInfos);
        }
        if (shouldShowPdfProviders()) {
            final List<ResolveInfo> pdfProviderResolveInfos = queryPdfProviders(this);
            pdfProviderItems = getPdfProviderItems(pdfProviderResolveInfos);
        }

        providerItems.addAll(imageProviderItems);
        if (!imageProviderItems.isEmpty() && !pdfProviderItems.isEmpty()) {
            providerItems.add(new ProvidersSeparatorItem());
        }
        providerItems.addAll(pdfProviderItems);

        ((GridLayoutManager) mFileProvidersView.getLayoutManager()).setSpanSizeLookup(
                new ProvidersSpanSizeLookup(providerItems, getGridSpanCount()));

        mFileProvidersView.setAdapter(new ProvidersAdapter(this, providerItems,
                item -> launchApp(item)));
    }

    private void launchApp(@NonNull final ProvidersAppItem item) {
        final Intent intent = item.getIntent();
        intent.setClassName(
                item.getResolveInfo().activityInfo.packageName,
                item.getResolveInfo().activityInfo.name);
        startActivityForResult(intent, REQ_CODE_CHOOSE_FILE);
    }


    private boolean shouldShowImageProviders() {
        return mDocImportEnabledFileTypes == DocumentImportEnabledFileTypes.IMAGES
                || mDocImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF_AND_IMAGES;
    }

    private boolean shouldShowPdfProviders() {
        return mDocImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF
                || mDocImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF_AND_IMAGES;
    }

    private List<ProvidersItem> getImageProviderItems(
            final List<ResolveInfo> imagePickerResolveInfos,
            final List<ResolveInfo> imageProviderResolveInfos) {
        final List<ProvidersItem> providerItems = new ArrayList<>();
        if (!imagePickerResolveInfos.isEmpty()
                || !imageProviderResolveInfos.isEmpty()) {
            providerItems.add(new ProvidersSectionItem(
                    getString(R.string.gc_file_chooser_fotos_section_header)));
            final Intent imagePickerIntent = createImagePickerIntent();
            for (final ResolveInfo imagePickerResolveInfo : imagePickerResolveInfos) {
                providerItems.add(new ProvidersAppItem(imagePickerIntent, imagePickerResolveInfo)); // NOPMD
            }
            final Intent getImageDocumentIntent = createGetImageDocumentIntent();
            for (final ResolveInfo imageProviderResolveInfo : imageProviderResolveInfos) {
                providerItems.add(
                        new ProvidersAppItem(getImageDocumentIntent, imageProviderResolveInfo)); // NOPMD
            }
        }
        return providerItems;
    }

    private List<ProvidersItem> getPdfProviderItems(
            final List<ResolveInfo> pdfProviderResolveInfos) {
        final List<ProvidersItem> providerItems = new ArrayList<>();
        if (!pdfProviderResolveInfos.isEmpty()) {
            providerItems.add(new ProvidersSectionItem(
                    getString(R.string.gc_file_chooser_pdfs_section_header)));
            final Intent getPdfDocumentIntent = createGetPdfDocumentIntent();
            for (final ResolveInfo pdfProviderResolveInfo : pdfProviderResolveInfos) {
                providerItems.add(
                        new ProvidersAppItem(getPdfDocumentIntent, pdfProviderResolveInfo)); // NOPMD
            }
        }
        return providerItems;
    }

    @NonNull
    private static List<ResolveInfo> queryImagePickers(@NonNull final Context context) {
        final Intent intent = createImagePickerIntent();

        return context.getPackageManager().queryIntentActivities(intent, 0);
    }

    @NonNull
    private static Intent createImagePickerIntent() {
        final Intent intent = new Intent(ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MimeType.IMAGE_WILDCARD.asString());
        if (isMultiPageEnabled()) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return intent;
    }

    @NonNull
    private static List<ResolveInfo> queryImageProviders(@NonNull final Context context) {
        final Intent intent = createGetImageDocumentIntent();

        return context.getPackageManager().queryIntentActivities(intent, 0);
    }

    @NonNull
    private static Intent createGetImageDocumentIntent() {
        final Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MimeType.IMAGE_WILDCARD.asString());
        if (isMultiPageEnabled()) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return intent;
    }

    @NonNull
    private static List<ResolveInfo> queryPdfProviders(@NonNull final Context context) {
        final Intent intent = createGetPdfDocumentIntent();

        return context.getPackageManager().queryIntentActivities(intent, 0);
    }

    @NonNull
    private static Intent createGetPdfDocumentIntent() {
        final Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MimeType.APPLICATION_PDF.asString());
        return intent;
    }
}
