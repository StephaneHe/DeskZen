package com.deskzen.ui.contacts

import android.graphics.BitmapFactory
import android.provider.ContactsContract
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Save contact info as primitives so they survive activity recreation (photo picker)
    var savedName by rememberSaveable { mutableStateOf(existingContact?.contactName ?: "") }
    var savedPhone by rememberSaveable { mutableStateOf(existingContact?.phoneNumber ?: "") }
    var savedAction by rememberSaveable { mutableStateOf(existingContact?.action?.name ?: "") }
    var savedContactPhotoUri by rememberSaveable { mutableStateOf(existingContact?.photoUri) }
    var customPhotoUri by rememberSaveable { mutableStateOf<String?>(existingContact?.photoUri) }
    var originalPhotoUri by rememberSaveable { mutableStateOf<String?>(existingContact?.originalPhotoUri) }
    // Dialog steps: "contact_picker", "action_picker", "photo_preview"
    var step by rememberSaveable { mutableStateOf(if (existingContact != null) "action_picker" else "contact_picker") }
    // Pending photo from picker (before confirmation) — always the ORIGINAL full image
    var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }

    // Reconstruct selectedContact from saved primitives
    var selectedContact by remember { mutableStateOf<PhoneContact?>(null) }
    if (selectedContact == null && savedName.isNotEmpty()) {
        selectedContact = PhoneContact(0, savedName, savedPhone, savedContactPhotoUri)
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val fileName = "contact_photo_${System.currentTimeMillis()}.jpg"
                    val destFile = java.io.File(context.filesDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    pendingPhotoUri = android.net.Uri.fromFile(destFile).toString()
                    step = "photo_preview"
                } catch (e: Exception) {
                    Timber.e(e, "Failed to copy contact photo")
                }
            }
        }
    }

    if (existingContact != null && selectedContact == null && savedName.isEmpty()) {
        savedName = existingContact.contactName
        savedPhone = existingContact.phoneNumber
        savedAction = existingContact.action.name
        savedContactPhotoUri = existingContact.photoUri
        selectedContact = PhoneContact(0, savedName, savedPhone, savedContactPhotoUri)
    }

    /** Helper to save the contact with current state */
    fun saveContact(action: ContactAction) {
        val contact = selectedContact ?: return
        savedAction = action.name
        onSave(
            QuickContact(
                position = 0,
                contactName = contact.name,
                phoneNumber = contact.phone,
                photoUri = customPhotoUri,
                originalPhotoUri = originalPhotoUri,
                action = action
            )
        )
    }

    when (step) {
        "contact_picker" -> {
            ContactPickerDialog(
                onSelect = { contact ->
                    selectedContact = contact
                    savedName = contact.name
                    savedPhone = contact.phone
                    // Copy contacts-DB photo to internal storage immediately for stable file:// URI
                    val stablePhotoUri = contact.photoUri?.let { uriStr ->
                        try {
                            val srcUri = android.net.Uri.parse(uriStr)
                            val fileName = "contact_photo_${System.currentTimeMillis()}.jpg"
                            val destFile = java.io.File(context.filesDir, fileName)
                            context.contentResolver.openInputStream(srcUri)?.use { input ->
                                destFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (destFile.exists() && destFile.length() > 0)
                                android.net.Uri.fromFile(destFile).toString()
                            else uriStr
                        } catch (e: Exception) {
                            uriStr
                        }
                    }
                    savedContactPhotoUri = stablePhotoUri
                    customPhotoUri = stablePhotoUri
                    step = "action_picker"
                },
                onDismiss = onDismiss
            )
        }
        "action_picker" -> {
            ActionPickerDialog(
                contactName = selectedContact?.name ?: savedName,
                photoUri = customPhotoUri,
                existingAction = if (savedAction.isNotEmpty()) {
                    try { ContactAction.valueOf(savedAction) } catch (_: Exception) { null }
                } else existingContact?.action,
                isEditing = existingContact != null,
                onSelectAction = { action -> saveContact(action) },
                onChangePhoto = {
                    if (originalPhotoUri != null) {
                        // Re-edit existing photo — only if file still exists on disk
                        val uri = android.net.Uri.parse(originalPhotoUri)
                        val fileExists = uri.scheme == "file" &&
                            uri.path != null &&
                            java.io.File(uri.path!!).exists()
                        if (fileExists) {
                            pendingPhotoUri = originalPhotoUri
                            step = "photo_preview"
                        } else {
                            // File gone after restart — fall back to picker
                            val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                type = "image/*"
                            }
                            photoPickerLauncher.launch(
                                android.content.Intent.createChooser(pickIntent, "Choisir une photo")
                            )
                        }
                    } else {
                        // Pick new photo
                        val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                            type = "image/*"
                        }
                        photoPickerLauncher.launch(
                            android.content.Intent.createChooser(pickIntent, "Choisir une photo")
                        )
                    }
                },
                onDelete = if (existingContact != null) onDelete else null,
                onBack = {
                    step = "contact_picker"
                    selectedContact = null
                    savedName = ""
                },
                onDismiss = onDismiss
            )
        }
        "photo_preview" -> {
            PhotoPreviewDialog(
                photoUri = pendingPhotoUri,
                contactName = selectedContact?.name ?: savedName,
                onConfirmCropped = { croppedUri ->
                    // Save cropped photo and keep original for re-editing
                    customPhotoUri = croppedUri
                    originalPhotoUri = pendingPhotoUri
                    val action = if (savedAction.isNotEmpty()) {
                        try { ContactAction.valueOf(savedAction) } catch (_: Exception) { ContactAction.CALL_PHONE }
                    } else existingContact?.action ?: ContactAction.CALL_PHONE

                    val contact = selectedContact ?: return@PhotoPreviewDialog
                    onSave(
                        QuickContact(
                            position = 0,
                            contactName = contact.name,
                            phoneNumber = contact.phone,
                            photoUri = croppedUri,
                            originalPhotoUri = pendingPhotoUri,
                            action = action
                        )
                    )
                },
                onPickNew = {
                    // Pick a different photo
                    val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
                    photoPickerLauncher.launch(
                        android.content.Intent.createChooser(pickIntent, "Choisir une photo")
                    )
                },
                onCancel = {
                    pendingPhotoUri = null
                    step = "action_picker"
                }
            )
        }
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
    photoUri: String?,
    existingAction: ContactAction?,
    isEditing: Boolean,
    onSelectAction: (ContactAction) -> Unit,
    onChangePhoto: () -> Unit,
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

                HorizontalDivider(
                    color = SoloPurple.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                // Change photo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangePhoto() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, null, tint = SoloElectricBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Modifier la photo", color = SoloElectricBlue)
                        if (photoUri != null) {
                            Text("Photo personnalisée", color = SoloTextMuted, fontSize = 11.sp)
                        } else {
                            Text("Aucune photo", color = SoloTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                if (isEditing) {
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

/**
 * Photo preview with draggable image inside circular mask.
 * User can pan the photo to choose framing. On confirm, the visible area is cropped and saved.
 */
@Composable
fun PhotoPreviewDialog(
    photoUri: String?,
    contactName: String,
    onConfirmCropped: (String) -> Unit,
    onPickNew: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Load bitmap asynchronously with downsampling to avoid OOM on large gallery photos
    var photoBitmap by remember(photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(photoUri) {
        photoBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (photoUri == null) return@withContext null
            try {
                val parsedUri = android.net.Uri.parse(photoUri)
                fun openStream(): java.io.InputStream? = if (parsedUri.scheme == "file") {
                    val path = parsedUri.path ?: return@openStream null
                    java.io.File(path).takeIf { it.exists() }?.inputStream()
                } else {
                    context.contentResolver.openInputStream(parsedUri)
                }
                // First pass: read bounds only for downsampling
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }
                var sample = 1
                val maxDim = maxOf(opts.outWidth, opts.outHeight)
                while (maxDim / (sample * 2) > 1024) sample *= 2
                opts.inSampleSize = sample
                opts.inJustDecodeBounds = false
                openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }
            } catch (e: Throwable) { null }
        }
    }

    // Image offset for panning (in px) and zoom
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zoomFactor by remember { mutableStateOf(1f) }

    val circleSize = 160.dp
    val circleSizePx = with(density) { circleSize.toPx() }

    // Capture delegated state in local val so smart-casts work inside lambdas
    val currentBitmap = photoBitmap
    val bmpWidth = currentBitmap?.width?.toFloat() ?: 1f
    val bmpHeight = currentBitmap?.height?.toFloat() ?: 1f
    val baseScale = circleSizePx / minOf(bmpWidth, bmpHeight)

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = SoloSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(contactName, color = SoloGlow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "D\u00e9placez la photo pour ajuster",
                    color = SoloPurple,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Calculate scale with zoom
                val totalScale = baseScale * zoomFactor
                val scaledW = bmpWidth * totalScale
                val scaledH = bmpHeight * totalScale
                val maxOffsetX = ((scaledW - circleSizePx) / 2f).coerceAtLeast(0f)
                val maxOffsetY = ((scaledH - circleSizePx) / 2f).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(SoloPurple.copy(alpha = 0.3f))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // Zoom: clamp between 1x and 5x
                                zoomFactor = (zoomFactor * zoom).coerceIn(1f, 5f)
                                // Pan: recalc max offsets with new zoom
                                val newTotalScale = baseScale * zoomFactor
                                val newScaledW = bmpWidth * newTotalScale
                                val newScaledH = bmpHeight * newTotalScale
                                val newMaxX = ((newScaledW - circleSizePx) / 2f).coerceAtLeast(0f)
                                val newMaxY = ((newScaledH - circleSizePx) / 2f).coerceAtLeast(0f)
                                offsetX = (offsetX + pan.x).coerceIn(-newMaxX, newMaxX)
                                offsetY = (offsetY + pan.y).coerceIn(-newMaxY, newMaxY)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentBitmap != null) {
                        Image(
                            painter = BitmapPainter(currentBitmap.asImageBitmap()),
                            contentDescription = contactName,
                            modifier = Modifier
                                .requiredSize(
                                    width = with(density) { scaledW.toDp() },
                                    height = with(density) { scaledH.toDp() }
                                )
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        offsetX.toInt(),
                                        offsetY.toInt()
                                    )
                                },
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        Text(
                            contactName.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bmp = photoBitmap  // local capture for smart-cast
                if (bmp != null) {
                    // Crop the visible circular area and save
                    try {
                        val bmpW = bmp.width.toFloat()
                        val bmpH = bmp.height.toFloat()
                        val cropTotalScale = baseScale * zoomFactor

                        val visibleCenterX = (bmpW * cropTotalScale / 2f - offsetX)
                        val visibleCenterY = (bmpH * cropTotalScale / 2f - offsetY)

                        val srcCenterX = visibleCenterX / cropTotalScale
                        val srcCenterY = visibleCenterY / cropTotalScale
                        val srcRadius = (circleSizePx / 2f) / cropTotalScale

                        val cropX = (srcCenterX - srcRadius).coerceAtLeast(0f).toInt()
                            .coerceAtMost(bmpW.toInt() - 1)
                        val cropY = (srcCenterY - srcRadius).coerceAtLeast(0f).toInt()
                            .coerceAtMost(bmpH.toInt() - 1)
                        val cropSize = (srcRadius * 2f).toInt().coerceAtMost(
                            minOf(bmpW.toInt() - cropX, bmpH.toInt() - cropY)
                        ).coerceAtLeast(1)

                        val cropped = android.graphics.Bitmap.createBitmap(
                            bmp, cropX, cropY, cropSize, cropSize
                        )
                        val final256 = android.graphics.Bitmap.createScaledBitmap(cropped, 256, 256, true)

                        val fileName = "contact_photo_${System.currentTimeMillis()}.jpg"
                        val destFile = java.io.File(context.filesDir, fileName)
                        destFile.outputStream().use { out ->
                            final256.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        onConfirmCropped(android.net.Uri.fromFile(destFile).toString())
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to crop photo")
                        photoUri?.let { onConfirmCropped(it) }
                    }
                }
            }) {
                Text("Confirmer", color = SoloGlow)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) {
                    Text("Annuler", color = SoloTextMuted)
                }
                TextButton(onClick = onPickNew) {
                    Text("Autre photo", color = SoloElectricBlue)
                }
            }
        }
    )
}
