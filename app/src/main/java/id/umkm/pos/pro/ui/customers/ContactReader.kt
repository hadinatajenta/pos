package id.umkm.pos.pro.ui.customers

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

internal data class PickedContact(
    val name: String,
    val whatsappNumber: String?,
)

internal fun readPickedContact(context: Context, contactUri: Uri): PickedContact? {
    val resolver = context.contentResolver
    var contactId: String? = null
    var displayName: String? = null
    try {
        resolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getString(0)
                displayName = cursor.getString(1)
            }
        }
        val name = displayName?.trim()?.takeIf { candidate -> candidate.isNotEmpty() } ?: return null
        val phoneNumber = contactId?.let { id -> firstPhoneNumber(resolver, id) }
        return PickedContact(
            name = name,
            whatsappNumber = phoneNumber?.let { number -> normalizeWhatsappNumber(number) },
        )
    } catch (error: SecurityException) {
        return null
    }
}

private fun firstPhoneNumber(
    resolver: android.content.ContentResolver,
    contactId: String,
): String? = resolver.query(
    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
    arrayOf(contactId),
    null,
)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(0) else null
}
