package com.bc.notivault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bc.notivault.data.model.InteractionStatus

@Composable
fun StatusBadge(
    status: InteractionStatus,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false
) {
    val (bgColor, textColor, dotColor) = when (status) {
        InteractionStatus.POSTED -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            Color(0xFF3B82F6) // Blue
        )
        InteractionStatus.UPDATED -> Triple(
            Color(0xFFFEF3C7), // Amber 100
            Color(0xFF92400E), // Amber 800
            Color(0xFFF59E0B)  // Amber 500
        )
        InteractionStatus.DISMISSED -> Triple(
            Color(0xFFF3E8FF), // Purple 100
            Color(0xFF6B21A8), // Purple 800
            Color(0xFFA855F7)  // Purple 500
        )
        InteractionStatus.OPENED -> Triple(
            Color(0xFFDCFCE7), // Green 100
            Color(0xFF166534), // Green 800
            Color(0xFF22C55E)  // Green 500
        )
        InteractionStatus.REMOVED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Color(0xFF64748B)  // Slate
        )
        InteractionStatus.UNKNOWN -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            Color(0xFF94A3B8)
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(
                if (status == InteractionStatus.UNKNOWN) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                } else Modifier
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Confirmation indicator dot
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = status.displayName.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}
