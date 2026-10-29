package com.bc.notivault.ui.screens.appnotifications

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.ui.components.AppActionBottomSheet
import com.bc.notivault.ui.components.AppIcon
import com.bc.notivault.ui.components.NotificationActionBottomSheet
import com.bc.notivault.ui.components.NotificationCard
import com.bc.notivault.ui.components.SwipeToDeleteContainer
import com.bc.notivault.ui.screens.dashboard.DashboardFilter
import com.bc.notivault.util.DateTimeUtils
import com.bc.notivault.util.rememberHaptic
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppNotificationsScreen(
    viewModel: AppNotificationsViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var actionNotification by remember { mutableStateOf<NotificationRecordEntity?>(null) }
    var showAppActionSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.click()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.click()
                        onNavigateToCalendar()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar View")
                    }

                    Box {
                        IconButton(onClick = {
                            haptic.click()
                            menuExpanded = true
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open App") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                                onClick = {
                                    haptic.click()
                                    menuExpanded = false
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(uiState.packageName)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (uiState.isExcluded) "Include in Recording" else "Exclude App") },
                                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                onClick = {
                                    haptic.tick()
                                    menuExpanded = false
                                    viewModel.toggleAppExclusion(!uiState.isExcluded)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // App Header Card (Phone 3 Mockup) - Supports long-press for app actions
            item(key = "app_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                haptic.longPress()
                                showAppActionSheet = true
                            }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = uiState.packageName, size = 52.dp)

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Metrics Row: Total & Today Cards (Phone 3 Mockup)
            item(key = "metrics_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppMetricCard(
                        count = uiState.totalCount,
                        label = "Total",
                        modifier = Modifier.weight(1f)
                    )

                    AppMetricCard(
                        count = uiState.todayCount,
                        label = "Today",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Exclude from recording row (Phone 3 Mockup)
            item(key = "exclude_row") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            haptic.tick()
                            viewModel.toggleAppExclusion(!uiState.isExcluded)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exclude from recording",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        Switch(
                            checked = uiState.isExcluded,
                            onCheckedChange = {
                                haptic.tick()
                                viewModel.toggleAppExclusion(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                uncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                uncheckedBorderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            // Search within this App
            item(key = "search_field") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search ${uiState.appName} notifications...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    haptic.click()
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Filter Chips + Calendar Date Picker Trigger
            item(key = "filter_chips_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips (All, Today, Starred, Ongoing, Dismissed)
                    DashboardFilter.values().forEach { filter ->
                        FilterChip(
                            selected = uiState.currentFilter == filter,
                            onClick = {
                                haptic.click()
                                viewModel.onFilterChange(filter)
                            },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Date Picker Chip
                    FilterChip(
                        selected = uiState.selectedDate != null,
                        onClick = {
                            haptic.click()
                            showDatePickerDialog = true
                        },
                        label = {
                            Text(
                                if (uiState.selectedDate != null)
                                    DateTimeUtils.formatDate(uiState.selectedDate!!)
                                else
                                    "Pick Date"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (uiState.selectedDate != null) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear date filter",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            haptic.click()
                                            viewModel.onDateSelected(null)
                                        }
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Active Date Filter Banner if filtered by a date
            if (uiState.selectedDate != null) {
                item(key = "active_date_banner") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing notifications for ${DateTimeUtils.formatDate(uiState.selectedDate!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(onClick = {
                                haptic.click()
                                viewModel.onDateSelected(null)
                            }) {
                                Text("Clear Filter", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Empty state
            if (uiState.notifications.isEmpty()) {
                item(key = "empty_notifications") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedDate != null)
                                "No matching notifications"
                            else
                                "No notifications captured for ${uiState.appName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Notifications posted by this application will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Notification Cards Feed (matching Phone 3) with swipe-to-delete (left-to-right) and long press support
            items(
                items = uiState.notifications,
                key = { it.id }
            ) { record ->
                SwipeToDeleteContainer(
                    onDelete = {
                        viewModel.deleteNotification(record.id)
                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    cornerRadius = 14.dp
                ) {
                    AppDetailNotificationCard(
                        record = record,
                        onClick = {
                            haptic.click()
                            onNavigateToDetail(record.id)
                        },
                        onLongClick = {
                            haptic.longPress()
                            actionNotification = record
                        }
                    )
                }
            }
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    haptic.click()
                    showDatePickerDialog = false
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(it)
                    }
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.click()
                    showDatePickerDialog = false
                }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Long-Press Notification Action Sheet
    NotificationActionBottomSheet(
        record = actionNotification,
        onDismiss = { actionNotification = null },
        onToggleStar = { viewModel.toggleStar(it.id, !it.isStarred) },
        onDelete = { viewModel.deleteNotification(it.id) },
        onExcludeApp = { viewModel.toggleAppExclusion(true) }
    )

    // Long-Press App Action Sheet
    if (showAppActionSheet) {
        AppActionBottomSheet(
            packageName = uiState.packageName,
            appName = uiState.appName,
            onDismiss = { showAppActionSheet = false },
            onViewHistory = { showAppActionSheet = false },
            onExcludeApp = { viewModel.toggleAppExclusion(!uiState.isExcluded) }
        )
    }
}

@Composable
private fun AppMetricCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDetailNotificationCard(
    record: NotificationRecordEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Dot indicator + Title + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cyan dot indicator matching phone 3 design
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (record.isStarred) Color(0xFFFACC15) else Color(0xFF38BDF8))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = record.title ?: record.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = DateTimeUtils.formatTime(record.postTime),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Message text
            val body = record.bigText ?: record.text
            if (!body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Optional Image Attachment thumbnail
            if (record.hasImageAttachment && !record.imageFilePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = File(record.imageFilePath),
                    contentDescription = "Notification attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
