package com.bc.notivault.data.model

data class AppStatInfo(
    val packageName: String,
    val appName: String,
    val notificationCount: Int,
    val latestPostTime: Long
)
