package com.bc.notivault.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bc.notivault.ui.components.AppIcon
import com.bc.notivault.ui.components.StatusBadge
import com.bc.notivault.util.DateTimeUtils
import com.bc.notivault.util.rememberHaptic
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val record by viewModel.notification.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Notification Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.click()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    record?.let { r ->
                        IconButton(onClick = {
                            haptic.confirm()
                            viewModel.toggleStar(!r.isStarred)
                        }) {
                            Icon(
                                imageVector = if (r.isStarred) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Star",
                                tint = if (r.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            haptic.reject()
                            viewModel.deleteNotification(onBack)
                            Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val currentRecord = record
        if (currentRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading record...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: App Icon, App Name, Package Name, Open App button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            packageName = currentRecord.packageName,
                            size = 50.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentRecord.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentRecord.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Open App Button
                        IconButton(
                            onClick = {
                                haptic.click()
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(currentRecord.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open App",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Notification Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CONTENT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        if (!currentRecord.title.isNullOrBlank()) {
                            Text(
                                text = currentRecord.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!currentRecord.text.isNullOrBlank()) {
                            Text(
                                text = currentRecord.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!currentRecord.bigText.isNullOrBlank() && currentRecord.bigText != currentRecord.text) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Expanded Content:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentRecord.bigText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!currentRecord.subText.isNullOrBlank()) {
                            Text(
                                text = "Subtext: ${currentRecord.subText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        if (!currentRecord.conversationTitle.isNullOrBlank()) {
                            Text(
                                text = "Conversation: ${currentRecord.conversationTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Image attachment if available
                        if (currentRecord.hasImageAttachment && currentRecord.imageFilePath != null) {
                            val imgFile = remember(currentRecord.imageFilePath) { File(currentRecord.imageFilePath) }
                            if (imgFile.exists()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = imgFile,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // Interaction & Lifecycle Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "INTERACTION & LIFECYCLE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(status = currentRecord.interactionStatus)
                            Text(
                                text = currentRecord.interactionStatus.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentRecord.removalTime != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            MetaItem(
                                label = "Removed At",
                                value = DateTimeUtils.formatFullDate(currentRecord.removalTime)
                            )
                            MetaItem(
                                label = "Removal Reason",
                                value = currentRecord.removalReasonText ?: "Unknown reason (Code: ${currentRecord.removalReason})"
                            )
                        }
                    }
                }

                // Technical Metadata Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "METADATA & TIMESTAMPS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        MetaItem(label = "Posted At", value = DateTimeUtils.formatFullDate(currentRecord.postTime))
                        MetaItem(label = "Captured At", value = DateTimeUtils.formatFullDate(currentRecord.captureTime))
                        if (currentRecord.lastUpdateTime != currentRecord.captureTime) {
                            MetaItem(label = "Updated At", value = DateTimeUtils.formatFullDate(currentRecord.lastUpdateTime))
                        }
                        if (!currentRecord.channelId.isNullOrBlank()) {
                            MetaItem(label = "Channel ID", value = currentRecord.channelId)
                        }
                        if (!currentRecord.category.isNullOrBlank()) {
                            MetaItem(label = "Category", value = currentRecord.category)
                        }
                        if (!currentRecord.groupKey.isNullOrBlank()) {
                            MetaItem(label = "Group Key", value = currentRecord.groupKey)
                        }
                        MetaItem(label = "Is Ongoing", value = if (currentRecord.isOngoing) "Yes" else "No")
                        MetaItem(label = "Is Group Summary", value = if (currentRecord.isGroupSummary) "Yes" else "No")

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Text(
                            text = "Deduplication Key:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentRecord.notificationKey,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Action Buttons: Copy, Share, Exclude App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.confirm()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val content = buildString {
                                appendLine("App: ${currentRecord.appName}")
                                if (!currentRecord.title.isNullOrBlank()) appendLine("Title: ${currentRecord.title}")
                                if (!currentRecord.text.isNullOrBlank()) appendLine("Text: ${currentRecord.text}")
                                if (!currentRecord.bigText.isNullOrBlank()) appendLine("Expanded: ${currentRecord.bigText}")
                                appendLine("Date: ${DateTimeUtils.formatFullDate(currentRecord.postTime)}")
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Notification Details", content))
                            Toast.makeText(context, "Full details copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.click()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${currentRecord.appName}: ${currentRecord.title ?: ""}")
                                putExtra(Intent.EXTRA_TEXT, "${currentRecord.appName}\n${currentRecord.title ?: ""}\n${currentRecord.bigText ?: currentRecord.text ?: ""}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Notification"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.reject()
                        viewModel.excludeApp(currentRecord.packageName)
                        Toast.makeText(context, "${currentRecord.appName} excluded from future recording", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exclude ${currentRecord.appName} from Recording")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
