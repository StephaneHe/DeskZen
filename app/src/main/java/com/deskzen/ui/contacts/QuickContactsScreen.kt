package com.deskzen.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.deskzen.domain.model.ContactAction
import com.deskzen.domain.model.QuickContact
import com.deskzen.ui.launcher.LauncherViewModel
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloPurple

@Composable
fun QuickContactsScreen(viewModel: LauncherViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val wallpaperBitmap by viewModel.wallpaperBitmap.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var configPosition by rememberSaveable { mutableIntStateOf(-1) }
    // Contact awaiting a deliberate confirmation before its action fires.
    // A single (possibly accidental) tap only arms this overlay — it never
    // starts a call/message on its own. See CallConfirmationOverlay.
    var pendingContact by remember { mutableStateOf<QuickContact?>(null) }

    // Permission launcher for READ_CONTACTS
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If granted, the dialog will be able to read contacts
    }

    // Permission launcher for CALL_PHONE
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF050810),
                                Color(0xFF0A1543),
                                Color(0xFF0D0D2B),
                                Color(0xFF1A0A2E),
                                Color(0xFF0A1543),
                                Color(0xFF050810)
                            )
                        )
                    )
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(8) { position ->
                val contact = uiState.quickContacts.getOrNull(position)
                if (contact != null) {
                    FilledContactSlot(
                        contact = contact,
                        onClick = {
                            // A tap NEVER calls directly — it only arms the
                            // confirmation overlay, which requires a second,
                            // deliberate press. Prevents accidental calls.
                            pendingContact = contact
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            configPosition = position
                        }
                    )
                } else {
                    EmptyContactSlot(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Check contacts permission first
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            } else {
                                configPosition = position
                            }
                        }
                    )
                }
            }
        }

        // Anti-accidental-call overlay, drawn on top of the grid. A single
        // (possibly accidental) tap only arms it; the action fires solely on a
        // deliberate confirmation, and auto-cancels on timeout / tap-outside.
        pendingContact?.let { contact ->
            CallConfirmationOverlay(
                contact = contact,
                onConfirm = {
                    val confirmed = contact
                    pendingContact = null
                    if (confirmed.action == ContactAction.CALL_PHONE &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    } else {
                        viewModel.executeContactAction(confirmed)
                    }
                },
                onCancel = { pendingContact = null }
            )
        }
    }

    // Contact config dialog
    if (configPosition >= 0) {
        val existingContact = uiState.quickContacts.getOrNull(configPosition)
        ContactConfigDialog(
            existingContact = existingContact,
            onSave = { contact ->
                viewModel.setQuickContact(configPosition, contact)
                configPosition = -1
            },
            onDelete = {
                viewModel.removeQuickContact(configPosition)
                configPosition = -1
            },
            onDismiss = { configPosition = -1 }
        )
    }
}

/**
 * Full-screen confirmation overlay that guards every speed-dial action.
 *
 * Rationale: the quick-contacts screen is meant for hands-free / in-car use,
 * where an accidental pocket/dashboard touch could otherwise fire a call. This
 * overlay enforces a deliberate two-step interaction: the first tap on a contact
 * only opens this dialog; the action runs only when the user presses the large,
 * unambiguous confirm button. It auto-cancels after [TIMEOUT_SECONDS] and on any
 * tap outside the card, so an armed call never lingers.
 */
private const val TIMEOUT_SECONDS = 10

@Composable
private fun CallConfirmationOverlay(
    contact: QuickContact,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var remaining by remember(contact) { mutableIntStateOf(TIMEOUT_SECONDS) }

    // Countdown that auto-cancels if the user does nothing.
    LaunchedEffect(contact) {
        remaining = TIMEOUT_SECONDS
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining--
        }
        onCancel()
    }

    val accent = when (contact.action) {
        ContactAction.CALL_PHONE -> SoloElectricBlue
        ContactAction.WHATSAPP_CALL -> Color(0xFF25D366)
        ContactAction.WHATSAPP_MESSAGE -> Color(0xFF25D366)
        ContactAction.SMS -> SoloPurple
    }
    val actionIcon = when (contact.action) {
        ContactAction.CALL_PHONE -> Icons.Default.Call
        ContactAction.WHATSAPP_CALL -> Icons.Default.Call
        ContactAction.WHATSAPP_MESSAGE -> Icons.Default.Chat
        ContactAction.SMS -> Icons.Default.Sms
    }
    val title = when (contact.action) {
        ContactAction.CALL_PHONE -> "Appeler"
        ContactAction.WHATSAPP_CALL -> "Appel WhatsApp"
        ContactAction.WHATSAPP_MESSAGE -> "Message WhatsApp"
        ContactAction.SMS -> "Envoyer un SMS"
    }
    val confirmLabel = when (contact.action) {
        ContactAction.CALL_PHONE, ContactAction.WHATSAPP_CALL -> "Appeler"
        ContactAction.WHATSAPP_MESSAGE, ContactAction.SMS -> "Envoyer"
    }
    val showNumber = contact.action == ContactAction.CALL_PHONE || contact.action == ContactAction.SMS

    // Scrim — tapping outside the card cancels.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        // Card — consumes taps so touching it never dismisses by accident.
        // Horizontal two-column layout: identity on the left, the large action
        // buttons on the right. This keeps the card short enough to fit the
        // landscape viewport (this screen is landscape-only) so the confirm and
        // cancel buttons are always fully on-screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF12121F))
                .border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume — do nothing */ }
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // Identity column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SoloPurple.copy(alpha = 0.3f))
                        .border(3.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.contactName.take(2).uppercase(),
                        style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = TextStyle(fontSize = 18.sp, color = accent, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.contactName,
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showNumber) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        style = TextStyle(fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Actions column
            Column(modifier = Modifier.width(300.dp)) {
                // Large, unambiguous confirm button.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent)
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = confirmLabel,
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                // Large cancel button with the auto-cancel countdown.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Annuler ($remaining)",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilledContactSlot(
    contact: QuickContact,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val photoBitmap = remember(contact.photoUri) {
        contact.photoUri?.let { uriString ->
            try {
                val parsedUri = android.net.Uri.parse(uriString)
                val inputStream = if (parsedUri.scheme == "file") {
                    java.io.File(parsedUri.path!!).inputStream()
                } else {
                    context.contentResolver.openInputStream(parsedUri)
                }
                inputStream?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (e: Exception) {
                null
            }
        }
    }

    val actionIcon = when (contact.action) {
        ContactAction.CALL_PHONE -> Icons.Default.Call
        ContactAction.WHATSAPP_CALL -> Icons.Default.Call
        ContactAction.WHATSAPP_MESSAGE -> Icons.Default.Chat
        ContactAction.SMS -> Icons.Default.Sms
    }
    val actionColor = when (contact.action) {
        ContactAction.CALL_PHONE -> SoloElectricBlue
        ContactAction.WHATSAPP_CALL -> Color(0xFF25D366)
        ContactAction.WHATSAPP_MESSAGE -> Color(0xFF25D366)
        ContactAction.SMS -> SoloPurple
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        // Photo
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SoloPurple.copy(alpha = 0.3f))
                .border(2.dp, actionColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoBitmap != null) {
                Image(
                    painter = BitmapPainter(photoBitmap),
                    contentDescription = contact.contactName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = contact.contactName.take(2).uppercase(),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Name
        Text(
            text = contact.contactName,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), offset = Offset(0f, 1f), blurRadius = 3f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Action icon
        Icon(
            imageVector = actionIcon,
            contentDescription = contact.action.name,
            modifier = Modifier.size(14.dp),
            tint = actionColor
        )
    }
}

@Composable
fun EmptyContactSlot(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = 2.dp,
                    color = SoloPurple.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Ajouter un contact",
                modifier = Modifier.size(32.dp),
                tint = SoloPurple.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "",
            style = TextStyle(fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}
