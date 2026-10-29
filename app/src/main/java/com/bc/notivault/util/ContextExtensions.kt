package com.bc.notivault.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses context wrappers to find the hosting [Activity].
 */
fun Context.findActivity(): Activity? {
    var currentContext: Context = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
