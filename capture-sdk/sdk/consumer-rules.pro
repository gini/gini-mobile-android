-keep class net.gini.android.capture.network.model.GiniCaptureReturnReason
-keep class net.gini.android.capture.Document
-keep class net.gini.android.capture.error.ErrorType
-keep class net.gini.android.capture.DocumentImportEnabledFileTypes

# Keep compound extraction and its serialization helpers for CX payments
-keep class net.gini.android.capture.network.model.GiniCaptureCompoundExtraction { *; }
-keep class net.gini.android.capture.internal.util.BundleHelperKt { *; }
