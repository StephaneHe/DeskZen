package com.deskzen.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deskzen.ui.theme.SoloCyan
import com.deskzen.ui.theme.SoloDeepBlack
import com.deskzen.ui.theme.SoloElectricBlue
import com.deskzen.ui.theme.SoloPurple
import com.deskzen.ui.theme.SoloTextMuted
import kotlinx.coroutines.delay

data class ToggleState(
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val activeColor: Color = SoloCyan,
    val inactiveColor: Color = SoloTextMuted
)

fun isVpnActive(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
}

fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun isBluetoothEnabled(context: Context): Boolean {
    return try {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        adapter?.isEnabled == true
    } catch (e: Exception) { false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickTogglesBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var vpnActive by remember { mutableStateOf(false) }
    var wifiActive by remember { mutableStateOf(false) }
    var btActive by remember { mutableStateOf(false) }

    // Poll state every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            vpnActive = isVpnActive(context)
            wifiActive = isWifiConnected(context)
            btActive = isBluetoothEnabled(context)
            delay(3000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // VPN / Tailscale
        ToggleChip(
            state = ToggleState(
                label = "VPN",
                icon = Icons.Default.VpnKey,
                isActive = vpnActive,
                activeColor = SoloCyan,
                inactiveColor = SoloTextMuted
            ),
            onClick = {
                // Open Tailscale
                val intent = context.packageManager.getLaunchIntentForPackage("com.tailscale.ipn")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            },
            onLongClick = {
                // Open VPN settings
                val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
        )

        // WiFi
        ToggleChip(
            state = ToggleState(
                label = "WiFi",
                icon = Icons.Default.Wifi,
                isActive = wifiActive,
                activeColor = SoloElectricBlue
            ),
            onClick = {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
        )

        // Bluetooth
        ToggleChip(
            state = ToggleState(
                label = "BT",
                icon = Icons.Default.Bluetooth,
                isActive = btActive,
                activeColor = SoloPurple
            ),
            onClick = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
        )

        // Flashlight
        ToggleChip(
            state = ToggleState(
                label = "Lampe",
                icon = Icons.Default.FlashlightOn,
                isActive = false,
                activeColor = Color(0xFFFFD700)
            ),
            onClick = {
                toggleFlashlight(context)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToggleChip(
    state: ToggleState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = if (state.isActive) state.activeColor.copy(alpha = 0.2f)
    else SoloDeepBlack.copy(alpha = 0.5f)
    val iconColor = if (state.isActive) state.activeColor else state.inactiveColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = state.icon,
            contentDescription = state.label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = state.label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (state.isActive) FontWeight.Bold else FontWeight.Normal,
                color = iconColor
            )
        )
    }
}

private var flashlightOn = false

fun toggleFlashlight(context: Context) {
    try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        flashlightOn = !flashlightOn
        cameraManager.setTorchMode(cameraId, flashlightOn)
    } catch (e: Exception) {
        // Ignore
    }
}
