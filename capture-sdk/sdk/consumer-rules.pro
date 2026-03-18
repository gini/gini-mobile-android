-keep class net.gini.android.capture.network.model.GiniCaptureReturnReason
-keep class net.gini.android.capture.Document
-keep class net.gini.android.capture.error.ErrorType
-keep class net.gini.android.capture.DocumentImportEnabledFileTypes

# Keep BundleHelper for compound extraction serialization (used by GiniCaptureCompoundExtraction Parcelable)
-keep class net.gini.android.capture.internal.util.BundleHelperKt { *; }
