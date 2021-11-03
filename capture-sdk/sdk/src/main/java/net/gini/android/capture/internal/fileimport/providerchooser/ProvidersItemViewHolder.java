package net.gini.android.capture.internal.fileimport.providerchooser;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

abstract class ProvidersItemViewHolder extends RecyclerView.ViewHolder {

    @NonNull
    final ProvidersItem.FileProviderItemType type;

    ProvidersItemViewHolder(@NonNull final View itemView,
            @NonNull final ProvidersItem.FileProviderItemType type) {
        super(itemView);
        this.type = type;
    }
}
