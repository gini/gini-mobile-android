package net.gini.android.capture.tracking.useranalytics.properties

import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsValue

sealed class UserAnalyticsEventProperty(key: String, value: String) :
    AnalyticsKeyPairProperty(key, value) {

    data class DocumentType(private val docType: Type) :
        UserAnalyticsEventProperty("document_type", docType.analyticsValue) {
        enum class Type(val analyticsValue: String) {
            Image("image"),
            Pdf("pdf"),
            QrCode("qrcode"),
            Unknown("unknown")
        }
    }

    data class PartialDocumentId(val documentId: String) :
        UserAnalyticsEventProperty("partial_document_id", documentId)


    data class DocumentId(val documentId: String) :
        UserAnalyticsEventProperty("document_id", documentId)

    data class DocumentPageNumber(val documentPageNumber: Int) :
        UserAnalyticsEventProperty("document_page_number", documentPageNumber.toString())

    data class ErrorCode(val errorCode: String) :
        UserAnalyticsEventProperty("error_code", errorCode)

    data class ErrorType(val errorType: Type) :
        UserAnalyticsEventProperty("error_type", errorType.analyticsValue) {
        enum class Type(val analyticsValue: String) {
            Unknown("unknown"),
            NoInternet("no_internet"),
            Unauthorized("unauthorized"),
            Upload("upload"),
            Server("server"),
            Outage("outage"),
            Maintenance("maintenance"),
            FileImportGeneric("file_import_generic"),
            FileImportSize("file_import_size"),
            FileImportPageCount("file_import_page_count"),
            FileImportUnsupported("file_import_unsupported"),
            FileImportPassword("file_import_password"),
        }
    }

    data class ErrorMessage(val errorMessage: String) :
        UserAnalyticsEventProperty("error_message", errorMessage)

    data class FlashActive(val flashActive: Boolean) :
        UserAnalyticsEventProperty("flash_active", flashActive.mapToAnalyticsValue())

    data class IbanDetectionLayerVisible(val ibanDetectionLayerVisible: Boolean) :
        UserAnalyticsEventProperty(
            "iban_detection_layer_visible",
            ibanDetectionLayerVisible.mapToAnalyticsValue()
        )

    data class QrCodeValid(val qrCodeValid: Boolean) :
        UserAnalyticsEventProperty("qr_code_valid", qrCodeValid.mapToAnalyticsValue())

    data class Screen(val screen: UserAnalyticsScreen) :
        UserAnalyticsEventProperty("screen", screen.name)

    // region Help Screen
    data class HasCustomItems(val hasCustomItems: Boolean) :
        UserAnalyticsEventProperty("has_custom_items", hasCustomItems.mapToAnalyticsValue())

    data class HelpItems(val helpItems: List<String>) :
        UserAnalyticsEventProperty("help_items", helpItems.toString())

    data class ItemTapped(val itemTappedName: String) :
        UserAnalyticsEventProperty("item_tapped", itemTappedName)

    // endregion
    data class CustomOnboardingTitle(val customOnboardingTitle: String) :
        UserAnalyticsEventProperty("custom_onboarding_title", customOnboardingTitle)

    data class OnboardingHasCustomItems(val hasCustomItems: Boolean) :
        UserAnalyticsEventProperty("has_custom_items", hasCustomItems.mapToAnalyticsValue())

    // region Return Assistant
    data class SwitchActive(val switchActive: Boolean) :
        UserAnalyticsEventProperty("switch_active", switchActive.mapToAnalyticsValue())

    data class ItemsChanged(val differences: Set<DifferenceType>) :
        UserAnalyticsEventProperty(
            "items_changed",
            differences.map { it.analyticsName }.toString()
        ) {
        enum class DifferenceType(val analyticsName: String) {
            Name("name"),
            Quantity("quantity"),
            Price("price"),
        }
    }
    // endregion

    data class Status(val statusType: StatusType) :
        UserAnalyticsEventProperty("status", statusType.analyticsName) {
        enum class StatusType(val analyticsName: String) {
            Successful("successful")
        }
    }
}