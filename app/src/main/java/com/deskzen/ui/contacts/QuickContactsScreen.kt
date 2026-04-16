package com.deskzen.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var configPosition by remember { mutableIntStateOf(-1) }

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
    ) {
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
                            // Check CALL_PHONE permission for phone calls
                            if (contact.action == ContactAction.CALL_PHONE &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            } else {
                                viewModel.executeContactAction(contact)
                            }
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

@Composable
fun FilledContactSlot(
    contact: QuickContact,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val photoBitmap = remember(contact.photoUri) {
        contact.photoUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
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
            .clickable(onClick = onClick)
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
