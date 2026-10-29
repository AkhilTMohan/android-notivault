package com.bc.notivault.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.util.DateTimeUtils
import com.bc.notivault.util.rememberHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionBottomSheet(
    record: NotificationRecordEntity?,
    onDismiss: () -> Unit,
    onToggleStar: (NotificationRecordEntity) -> Unit,
    onDelete: (NotificationRecordEntity) -> Unit,
    onExcludeApp: (String) -> Unit,
    onOpenAppVault: ((String) -> Unit)? = null
) {
    if (record == null) return
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131B2E),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: App icon + App Name & preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = record.packageName, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val preview = record.title ?: record.text ?: record.bigText ?: ""
                    if (preview.isNotBlank()) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = DateTimeUtils.formatRelativeTime(record.postTime),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action 1: Copy Text
            BottomSheetActionRow(
                icon = Icons.Default.ContentCopy,
                title = "Copy Notification Content",
                subtitle = "Copy title and message text to clipboard",
                onClick = {
                    val fullText = buildString {
                        if (!record.title.isNullOrBlank()) append("${record.title}\n")
                        if (!record.text.isNullOrBlank()) append("${record.text}\n")
                        if (!record.bigText.isNullOrBlank() && record.bigText != record.text) {
                            append("${record.bigText}\n")
                        }
                    }.trim()
                    copyToClipboard(context, "Notification", fullText)
                    haptic.confirm()
                    onDismiss()
                }
            )

            // Action 2: Share
            BottomSheetActionRow(
                icon = Icons.Default.Share,
                title = "Share Notification",
                subtitle = "Send text to another application",
                onClick = {
                    shareNotification(context, record)
                    onDismiss()
                }
            )

            // Action 3: Star / Unstar
            BottomSheetActionRow(
                icon = if (record.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                title = if (record.isStarred) "Remove from Starred" else "Star Notification",
                subtitle = if (record.isStarred) "Remove bookmark" else "Bookmark for quick access",
                iconTint = if (record.isStarred) Color(0xFFFACC15) else MaterialTheme.colorScheme.primary,
                onClick = {
                    haptic.confirm()
                    onToggleStar(record)
                    val msg = if (record.isStarred) "Removed from Starred" else "Added to Starred"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Action 4: Open originating app
            BottomSheetActionRow(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Open ${record.appName}",
                subtitle = "Launch the application on this device",
                onClick = {
                    launchApp(context, record.packageName)
                    onDismiss()
                }
            )

            // Action 5: Open App Vault
            if (onOpenAppVault != null) {
                BottomSheetActionRow(
                    icon = Icons.Default.History,
                    title = "View App Notification History",
                    subtitle = "Browse all notifications captured for this app",
                    onClick = {
                        onDismiss()
                        onOpenAppVault(record.packageName)
                    }
                )
            }

            // Action 6: Exclude app from recording
            BottomSheetActionRow(
                icon = Icons.Default.Block,
                title = "Exclude ${record.appName}",
                subtitle = "Stop recording future notifications from this app",
                onClick = {
                    haptic.reject()
                    onExcludeApp(record.packageName)
                    Toast.makeText(context, "${record.appName} excluded from recording", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Action 7: Delete Record (Danger)
            BottomSheetActionRow(
                icon = Icons.Default.DeleteForever,
                title = "Delete Notification",
                subtitle = "Permanently remove this notification from NotiVault",
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    haptic.reject()
                    onDelete(record)
                    Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    packageName: String?,
    appName: String?,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
    onExcludeApp: () -> Unit
) {
    if (packageName == null) return
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayName = appName ?: packageName

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131B2E),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: App icon + App Name & package
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = packageName, size = 48.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action 1: View History
            BottomSheetActionRow(
                icon = Icons.Default.History,
                title = "View App Vault",
                subtitle = "Browse all notifications and search history",
                onClick = {
                    onDismiss()
                    onViewHistory()
                }
            )

            // Action 2: Launch App
            BottomSheetActionRow(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Open Application",
                subtitle = "Launch $displayName on your device",
                onClick = {
                    launchApp(context, packageName)
                    onDismiss()
                }
            )

            // Action 3: App System Notification Settings
            BottomSheetActionRow(
                icon = Icons.Default.Tune,
                title = "System Notification Settings",
                subtitle = "Manage Android system notification channels for this app",
                onClick = {
                    openSystemNotificationSettings(context, packageName)
                    onDismiss()
                }
            )

            // Action 4: Exclude App from Recording
            BottomSheetActionRow(
                icon = Icons.Default.Block,
                title = "Exclude from Vault",
                subtitle = "Ignore future notifications from $displayName",
                onClick = {
                    haptic.reject()
                    onExcludeApp()
                    Toast.makeText(context, "$displayName excluded from recording", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Action 5: Copy Package Name
            BottomSheetActionRow(
                icon = Icons.Default.ContentCopy,
                title = "Copy Package Identifier",
                subtitle = packageName,
                onClick = {
                    copyToClipboard(context, "Package Name", packageName)
                    haptic.confirm()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BottomSheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = Color.White
) {
    val haptic = rememberHaptic()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.click()
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareNotification(context: Context, record: NotificationRecordEntity) {
    val shareContent = buildString {
        appendLine("Notification from ${record.appName}")
        if (!record.title.isNullOrBlank()) appendLine("Title: ${record.title}")
        val body = record.bigText ?: record.text
        if (!body.isNullOrBlank()) appendLine("Message: $body")
        appendLine("Time: ${DateTimeUtils.formatRelativeTime(record.postTime)}")
    }.trim()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Notification from ${record.appName}")
        putExtra(Intent.EXTRA_TEXT, shareContent)
    }
    context.startActivity(Intent.createChooser(intent, "Share notification via"))
}

private fun launchApp(context: Context, packageName: String) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        Toast.makeText(context, "Cannot open $packageName", Toast.LENGTH_SHORT).show()
    }
}

private fun openSystemNotificationSettings(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open system settings", Toast.LENGTH_SHORT).show()
        }
    }
}
