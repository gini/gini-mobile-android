package net.gini.android.capture.network.test

import android.net.Uri
import net.gini.android.core.api.models.Document
import java.util.*

/**
 * Created by Alpár Szotyori on 18.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

fun bankAPIDocumentWithId(id: String) =
    Document(
        id,
        Document.ProcessingState.COMPLETED,
        "",
        1,
        Date(),
        Date(),
        Document.SourceClassification.SCANNED,
        Uri.EMPTY,
        emptyList(),
        emptyList()
    )