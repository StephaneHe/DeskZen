package com.deskzen.domain.model

data class QuickContact(
    val position: Int,
    val contactName: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val originalPhotoUri: String? = null,  // un-cropped original for re-editing
    val action: ContactAction = ContactAction.CALL_PHONE
)

enum class ContactAction {
    CALL_PHONE,
    WHATSAPP_CALL,
    WHATSAPP_MESSAGE,
    SMS
}
