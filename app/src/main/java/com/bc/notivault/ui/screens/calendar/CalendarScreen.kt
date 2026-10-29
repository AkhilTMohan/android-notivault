package com.bc.notivault.ui.screens.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bc.notivault.ui.components.AppIcon
import com.bc.notivault.ui.components.SwipeToDeleteContainer
import com.bc.notivault.ui.components.NotiVaultBottomNavBar
import com.bc.notivault.ui.navigation.Destinations
import com.bc.notivault.util.DateTimeUtils
import com.bc.notivault.util.rememberHaptic
import java.text.DateFormatSymbols
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
    onNavigateToAppNotifications: (String, Long) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDay = uiState.selectedDay

    val monthName = DateFormatSymbols().months[uiState.month]
    val monthTitle = "$monthName ${uiState.year}"

    val todayCal = Calendar.getInstance()
    val isCurrentYear = todayCal.get(Calendar.YEAR) == uiState.year
    val isCurrentMonth = todayCal.get(Calendar.MONTH) == uiState.month
    val todayDate = if (isCurrentYear && isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

    // Default to today or first day with notifications if not selected
    val activeDayData = selectedDay ?: run {
        if (todayDate > 0 && uiState.daysData.containsKey(todayDate)) {
            uiState.daysData[todayDate]
        } else {
            uiState.daysData.values.firstOrNull { it.totalCount > 0 }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NotiVaultBottomNavBar(
                currentRoute = Destinations.CALENDAR,
                onNavigateToTab = { route ->
                    when (route) {
                        Destinations.DASHBOARD -> onNavigateToHome()
                        Destinations.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Month Selector Header Card
            item(key = "month_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        haptic.tick()
                        viewModel.onPreviousMonth()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(onClick = {
                        haptic.tick()
                        viewModel.onNextMonth()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Days of Week Header
            item(key = "dow_header") {
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Calendar Grid
            item(key = "calendar_grid") {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    CalendarMonthGrid(
                        daysInMonth = uiState.daysInMonth,
                        firstDayOfWeek = uiState.firstDayOfWeek,
                        selectedDayNumber = activeDayData?.dayOfMonth ?: -1,
                        daysData = uiState.daysData,
                        onDayClick = { dayData ->
                            haptic.click()
                            viewModel.onSelectDay(dayData)
                        }
                    )
                }
            }

            // Day Breakdown Section Header (e.g. "15 Oct 2026")
            item(key = "day_section_header") {
                val headerDateText = activeDayData?.let {
                    DateTimeUtils.formatDateShort(it.dateTimestamp)
                } ?: "Selected Date"

                Text(
                    text = headerDateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Empty state for selected date
            if (activeDayData == null || activeDayData.apps.isEmpty()) {
                item(key = "no_apps_for_day") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "No notifications recorded on this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // List of apps on that day (Phone 2 style)
                items(
                    items = activeDayData.apps,
                    key = { it.packageName }
                ) { appInfo ->
                    SwipeToDeleteContainer(
                        onDelete = {
                            viewModel.deleteAppNotificationsForDay(appInfo.packageName, activeDayData.dateTimestamp)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        cornerRadius = 14.dp
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.click()
                                    onNavigateToAppNotifications(appInfo.packageName, activeDayData.dateTimestamp)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    packageName = appInfo.packageName,
                                    size = 42.dp
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appInfo.appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${appInfo.count} notifications",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    daysInMonth: Int,
    firstDayOfWeek: Int,
    selectedDayNumber: Int,
    daysData: Map<Int, DayData>,
    onDayClick: (DayData) -> Unit
) {
    // firstDayOfWeek in java.util.Calendar: Sunday = 1, Monday = 2, ...
    val emptyPrecedingDays = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
    val totalCells = emptyPrecedingDays + daysInMonth
    val numRows = (totalCells + 6) / 7

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until numRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - emptyPrecedingDays + 1

                    if (dayNumber in 1..daysInMonth) {
                        val data = daysData[dayNumber] ?: DayData(
                            dayOfMonth = dayNumber,
                            dateTimestamp = 0L,
                            totalCount = 0,
                            topPackageNames = emptyList(),
                            apps = emptyList()
                        )
                        CalendarDayCell(
                            dayData = data,
                            isSelected = dayNumber == selectedDayNumber,
                            onClick = { onDayClick(data) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayData: DayData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasNotifications = dayData.totalCount > 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circle around day number when selected (matching Phone 2 cyan circle)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${dayData.dayOfMonth}",
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Count under date (matching Phone 2)
        Text(
            text = if (hasNotifications) "${dayData.totalCount}" else "·",
            fontSize = 10.sp,
            fontWeight = if (hasNotifications) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else if (hasNotifications) MaterialTheme.colorScheme.onSurfaceVariant
            else Color.Transparent
        )
    }
}
