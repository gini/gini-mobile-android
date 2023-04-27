package net.gini.android.capture.help;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.gini.android.capture.help.SupportedFormatsAdapter.ItemType.FORMAT_INFO;
import static net.gini.android.capture.help.SupportedFormatsAdapter.ItemType.HEADER;
import static net.gini.android.capture.internal.util.FeatureConfiguration.getDocumentImportEnabledFileTypes;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isFileImportEnabled;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isQRCodeScanningEnabled;

/**
 * Internal use only.
 *
 * @suppress
 */
public class SupportedFormatsAdapter extends
        RecyclerView.Adapter<SupportedFormatsAdapter.FormatItemViewHolder> {

    private final List<Enum> mItems;
    private Boolean isQRDocument;

    public SupportedFormatsAdapter(Boolean isQrCodeDocument) {
        isQRDocument = isQrCodeDocument;
        mItems = setUpItems();
    }

    private List<Enum> setUpItems() {
        final ArrayList<Enum> items = new ArrayList<>();
        items.add(SectionHeader.SUPPORTED_FORMATS);
        if (isQRDocument) {
            items.add(SupportedFormat.QR_BEZAHL);
            items.add(SupportedFormat.QR_EPS);
            items.add(SupportedFormat.QR_STUZZA);
            items.add(SupportedFormat.QR_GIROCODE);
            return items;
        }

        items.add(SupportedFormat.PRINTED_INVOICES);
        if (isFileImportEnabled()
                || getDocumentImportEnabledFileTypes()
                == DocumentImportEnabledFileTypes.PDF_AND_IMAGES) {
            items.add(SupportedFormat.SINGLE_PAGE_AS_JPEG_PNG_GIF);
            items.add(SupportedFormat.PDF);
        } else if (getDocumentImportEnabledFileTypes()
                == DocumentImportEnabledFileTypes.PDF) {
            items.add(SupportedFormat.PDF);
        }
        if (isQRCodeScanningEnabled()) {
            items.add(SupportedFormat.QR_CODE);
        }
        items.add(SupportedFormat.PHOTOS_OF_MONITORS);
        items.add(SectionHeader.UNSUPPORTED_FORMATS);
        Collections.addAll(items, UnsupportedFormat.values());
        return items;
    }

    @Override
    public FormatItemViewHolder onCreateViewHolder(final ViewGroup parent,
            final int viewType) {
        final ItemType itemType = ItemType.fromOrdinal(viewType);
        switch (itemType) {
            case HEADER:
                return createHeaderItemViewHolder(parent);
            case FORMAT_INFO:
                return createFormatInfoItemViewHolder(parent);
            default:
                throw new IllegalStateException("Unknown ItemType: " + itemType);
        }
    }

    private FormatItemViewHolder createHeaderItemViewHolder(final ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.gc_item_format_header, parent, false);
        return new HeaderItemViewHolder(view);
    }

    private FormatItemViewHolder createFormatInfoItemViewHolder(final ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.gc_item_format_info, parent, false);
        return new FormatInfoItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FormatItemViewHolder holder,
            final int position) {
        if (holder instanceof HeaderItemViewHolder) {
            final HeaderItemViewHolder viewHolder = (HeaderItemViewHolder) holder;
            final SectionHeader sectionHeader = (SectionHeader) mItems.get(position);
            viewHolder.title.setText(sectionHeader.title);
        } else if (holder instanceof FormatInfoItemViewHolder) {
            final FormatInfoItemViewHolder viewHolder = (FormatInfoItemViewHolder) holder;
            final FormatInfo formatInfo = (FormatInfo) mItems.get(position);
            viewHolder.label.setText(formatInfo.getLabel());
            viewHolder.icon.setImageResource(formatInfo.getIcon());

            //Remove last dividers
            if (formatInfo.getLabel() == SupportedFormat.PHOTOS_OF_MONITORS.getLabel()) {
                viewHolder.dividerView.setVisibility(View.INVISIBLE);
            }
            else {
                viewHolder.dividerView.setVisibility(View.VISIBLE);
            }

            if (position == mItems.size() - 1) {
                viewHolder.dividerView.setVisibility(View.INVISIBLE);
            }

        }
    }

    @Override
    public int getItemViewType(final int position) {
        final Enum item = mItems.get(position);
        return item instanceof SectionHeader ? HEADER.ordinal() : FORMAT_INFO.ordinal();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    enum ItemType {
        HEADER,
        FORMAT_INFO;

        static ItemType fromOrdinal(final int ordinal) {
            if (ordinal >= values().length) {
                throw new IllegalArgumentException(
                        "Ordinal out of bounds: ordinal (" + ordinal
                                + ") was not less than nr of values (" + values().length + ")");
            }
            return values()[ordinal];
        }
    }

    private enum SectionHeader {
        SUPPORTED_FORMATS(R.string.gc_supported_format_section_header),
        UNSUPPORTED_FORMATS(R.string.gc_unsupported_format_section_header);

        @StringRes
        final int title;

        SectionHeader(@StringRes final int title) {
            this.title = title;
        }
    }

    private enum SupportedFormat implements FormatInfo {
        PRINTED_INVOICES(R.string.gc_supported_format_printed_invoices),
        SINGLE_PAGE_AS_JPEG_PNG_GIF(R.string.gc_supported_format_single_page_as_jpeg_png_gif),
        PDF(R.string.gc_supported_format_pdf),
        QR_CODE(R.string.gc_supported_format_qr_code),
        PHOTOS_OF_MONITORS(R.string.gc_photos_of_monitors_or_screens),
        // QR Code formats
        QR_BEZAHL(R.string.gc_supported_format_qr_type_bezahl),
        QR_EPS(R.string.gc_supported_format_qr_type_eps),
        QR_STUZZA(R.string.gc_supported_format_qr_type_stuzza),
        QR_GIROCODE(R.string.gc_supported_format_qr_type_girocode);

        @DrawableRes
        private final int mSupportedIcon;
        @StringRes
        private final int mLabel;

        SupportedFormat(@StringRes final int label) {
            mLabel = label;
            mSupportedIcon = R.drawable.gc_format_info_supported_icon;
        }

        @Override
        @DrawableRes
        public int getIcon() {
            return mSupportedIcon;
        }

        @Override
        @StringRes
        public int getLabel() {
            return mLabel;
        }
    }

    private enum UnsupportedFormat implements FormatInfo {

        HANDWRITING(R.string.gc_unsupported_format_handwriting);

        @DrawableRes
        private final int mIcon;
        @StringRes
        private final int mLabel;

        UnsupportedFormat(@StringRes final int label) {
            mLabel = label;
            mIcon = R.drawable.gc_format_info_unsupported_icon;
        }

        @Override
        @DrawableRes
        public int getIcon() {
            return mIcon;
        }


        @Override
        @StringRes
        public int getLabel() {
            return mLabel;
        }
    }

    private class FormatInfoItemViewHolder extends FormatItemViewHolder {

        final ImageView icon;
        final TextView label;
        final View dividerView;

        FormatInfoItemViewHolder(final View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.gc_format_info_item_label);
            icon = itemView.findViewById(R.id.gc_format_info_item_icon);
            dividerView = itemView.findViewById(R.id.gc_item_format_divider);
        }
    }

    class FormatItemViewHolder extends RecyclerView.ViewHolder {

        FormatItemViewHolder(final View itemView) {
            super(itemView);
        }
    }

    private class HeaderItemViewHolder extends FormatItemViewHolder {

        final TextView title;

        HeaderItemViewHolder(final View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.gc_supported_formats_item_header);
        }
    }

    interface FormatInfo {
        @DrawableRes
        int getIcon();

        @StringRes
        int getLabel();
    }
}
