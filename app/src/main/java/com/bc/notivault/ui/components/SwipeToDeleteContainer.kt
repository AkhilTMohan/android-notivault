package com.bc.notivault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bc.notivault.util.rememberHaptic
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Wraps notification cards with a safe swipe-to-reveal delete gesture.
 * Swiping (left or right) reveals a dedicated Delete button with a trash icon.
 * Deletion is only triggered when the user explicitly taps the Delete button,
 * completely preventing accidental deletions from swipes alone.
 * Tapping the card or swiping back smoothly closes the revealed action.
 */
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    val haptic = rememberHaptic()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val actionWidth = 88.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val offsetX = remember { Animatable(0f) }
    var dragStartOffset by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val isRevealed = abs(offsetX.value) >= actionWidthPx * 0.4f

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 250),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxWidth()
        ) {
            // Background Action Layer (revealed under the card)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFFDC2626))
            ) {
                val progress = (abs(offsetX.value) / actionWidthPx).coerceIn(0f, 1f)
                val revealedWidth = maxOf(actionWidth, with(density) { abs(offsetX.value).toDp() })

                if (offsetX.value < 0) {
                    // Swiped left -> Revealed on right
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(revealedWidth)
                            .fillMaxHeight()
                            .clickable(
                                enabled = isRevealed && !isDeleting,
                                onClick = {
                                    if (!isDeleting) {
                                        isDeleting = true
                                        haptic.reject()
                                        isDismissed = true
                                        onDelete()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = progress
                                scaleX = 0.7f + 0.3f * progress
                                scaleY = 0.7f + 0.3f * progress
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete notification",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (offsetX.value > 0) {
                    // Swiped right -> Revealed on left
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(revealedWidth)
                            .fillMaxHeight()
                            .clickable(
                                enabled = isRevealed && !isDeleting,
                                onClick = {
                                    if (!isDeleting) {
                                        isDeleting = true
                                        haptic.reject()
                                        isDismissed = true
                                        onDelete()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = progress
                                scaleX = 0.7f + 0.3f * progress
                                scaleY = 0.7f + 0.3f * progress
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete notification",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Foreground Content Layer (slides horizontally to reveal delete button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(actionWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartOffset = offsetX.value
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (offsetX.value + dragAmount).coerceIn(
                                    -actionWidthPx * 1.35f,
                                    actionWidthPx * 1.35f
                                )
                                coroutineScope.launch {
                                    offsetX.snapTo(newOffset)
                                }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    val current = offsetX.value
                                    val target = when {
                                        current > actionWidthPx * 0.5f -> actionWidthPx
                                        current < -actionWidthPx * 0.5f -> -actionWidthPx
                                        dragStartOffset < -actionWidthPx * 0.5f && current > dragStartOffset + actionWidthPx * 0.25f -> 0f
                                        dragStartOffset > actionWidthPx * 0.5f && current < dragStartOffset - actionWidthPx * 0.25f -> 0f
                                        dragStartOffset == 0f && current > actionWidthPx * 0.35f -> actionWidthPx
                                        dragStartOffset == 0f && current < -actionWidthPx * 0.35f -> -actionWidthPx
                                        abs(dragStartOffset) > actionWidthPx * 0.5f -> if (dragStartOffset > 0) actionWidthPx else -actionWidthPx
                                        else -> 0f
                                    }
                                    if (target != 0f && dragStartOffset == 0f) {
                                        haptic.tick()
                                    }
                                    offsetX.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f)
                                }
                            }
                        )
                    }
            ) {
                content()

                // If revealed, intercept taps on the card to safely dismiss the reveal instead of navigating
                if (isRevealed) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.tick()
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}
