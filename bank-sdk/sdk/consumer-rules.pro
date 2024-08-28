-keep class net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
-keep class net.gini.android.bank.sdk.capture.skonto.model.SkontoData { *; }

# Keep Parcelable classes untouced
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}