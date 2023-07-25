package net.gini.android.core.api.test

import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.GiniApiType
import kotlin.coroutines.CoroutineContext

/**
 * Created by Alpár Szotyori on 21.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */
class DocumentRemoteSourceForTests(
    coroutineContext: CoroutineContext,
    documentService: DocumentService,
    giniApiType: GiniApiType,
    baseUriString: String
) : DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {


}