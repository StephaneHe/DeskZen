package com.deskzen.ui.contacts

import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deskzen.domain.model.ContactAction
import com.deskzen.domain.model.QuickContact
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloGlow
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloSurface
import com.deskzen.ui.theme.SoloTextMuted

data class PhoneContact(
    val id: Long,
    val name: String,
    val phone: String,
    val photoUri: String?
)

@Composable
fun ContactConfigDialog(
    existingContact: QuickContact?,
    onSave: (QuickContact) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Two-step dialog: 1) pick contact, 2) pick action
    var selectedContact by remember { mutableStateOf<PhoneContact?>(null) }
    var showActionPicker by remember { mutableStateOf(existingContact != null) }

    if (existingContact != null && selectedContact == null) {
        // Editing existing — go straight to action picker
        selectedContact = PhoneContact(
            id = 0,
            name = existingContact.contactName,
            phone = existingContact.phoneNumber,
            photoUri = existingContact.photoUri
        )
    }

    if (!showActionPicker) {
        ContactPickerDialog(
            onSelect = { contact ->
                selectedContact = contact
                showActionPicker = true
            },
            onDismiss = onDismiss
        )
    } else {
        ActionPickerDialog(
            contactName = selectedContact?.name ?: "",
            existingAction = existingContact?.action,
            isEditing = existingContact != null,
            onSelectAction = { action ->
                val contact = selectedContact ?: return@ActionPickerDialog
                onSave(
                    QuickContact(
                        position = 0, // Will be set by caller
                        contactName = contact.name,
                        phoneNumber = contact.phone,
                        photoUri = contact.photoUri,
                        action = action
                    )
                )
            },
            onDelete = if (existingContact != null) onDelete else null,
            onBack = {
                showActionPicker = false
                selectedContact = null
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ContactPickerDialog(
    onSelect: (PhoneContact) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }

    // Load contacts
    LaunchedEffect(Unit) {
        val result = mutableListOf<PhoneContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                result.add(
                    PhoneContact(
                        id = it.getLong(0),
                        name = it.getString(1) ?: "Inconnu",
                        phone = it.getString(2) ?: "",
                        photoUri = it.getString(3)
                    )
                )
            }
        }
        contacts = result.distinctBy { it.phone }
    }

    val filtered = if (searchQuery.isBlank()) contacts
    else contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Choisir un contact", color = SoloGlow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoloElectricBlue,
                        unfocusedBorderColor = SoloPurple.copy(alpha = 0.3f),
                        cursorColor = SoloElectricBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(filtered) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(contact) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SoloElectricBlue,
                                modifier = Modifier.width(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(contact.name, color = Color.White, fontSize = 14.sp)
                                Text(contact.phone, color = SoloTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SoloTextMuted)
            }
        }
    )
}

@Composable
fun ActionPickerDialog(
    contactName: String,
    existingAction: ContactAction?,
    isEditing: Boolean,
    onSelectAction: (ContactAction) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    data class ActionOption(
        val action: ContactAction,
        val label: String,
        val icon: ImageVector,
        val color: Color
    )

    val options = listOf(
        ActionOption(ContactAction.CALL_PHONE, "Appel t\u00e9l\u00e9phone", Icons.Default.Call, SoloElectricBlue),
        ActionOption(ContactAction.WHATSAPP_CALL, "Appel WhatsApp", Icons.Default.Call, Color(0xFF25D366)),
        ActionOption(ContactAction.WHATSAPP_MESSAGE, "Message WhatsApp", Icons.Default.Chat, Color(0xFF25D366)),
        ActionOption(ContactAction.SMS, "SMS", Icons.Default.Sms, SoloPurple)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(contactName, color = SoloGlow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    if (isEditing) "Modifier l'action" else "Action par d\u00e9faut",
                    color = SoloPurple,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAction(option.action) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = option.color,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option.label,
                            color = if (existingAction == option.action) option.color else Color.White,
                            fontWeight = if (existingAction == option.action) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (isEditing) {
                    HorizontalDivider(
                        color = SoloPurple.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    // Change contact
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBack() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = SoloElectricBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Changer le contact", color = SoloElectricBlue)
                    }
                }

                if (onDelete != null) {
                    HorizontalDivider(
                        color = SoloPurple.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDelete() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFE53935))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Supprimer", color = Color(0xFFE53935))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SoloTextMuted)
            }
        }
    )
}
