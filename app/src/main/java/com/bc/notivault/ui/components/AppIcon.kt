package com.bc.notivault.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

private val iconCache = ConcurrentHashMap<String, Drawable>()

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val drawable = remember(packageName) {
        getAppIcon(context, packageName)
    }

    if (drawable != null) {
        val bitmap = remember(drawable) {
            drawable.toBitmap(
                width = (size.value * 2).toInt().coerceAtLeast(48),
                height = (size.value * 2).toInt().coerceAtLeast(48)
            ).asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = packageName,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

private fun getAppIcon(context: Context, packageName: String): Drawable? {
    iconCache[packageName]?.let { return it }
    return try {
        val pm = context.packageManager
        val icon = pm.getApplicationIcon(packageName)
        iconCache[packageName] = icon
        icon
    } catch (_: Exception) {
        null
    }
}
