package net.gini.pay.ginipaybusiness.review.error

class NoBankSelected : Throwable("No Bank Selected")

/**
 * Thrown when there's no Payment Provider for the package name of the selected bank app.
 */
class NoProviderForPackageName(packageName: String) : Throwable("No Provide for package $packageName")