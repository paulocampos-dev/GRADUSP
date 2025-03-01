package com.prototype.gradusp.utils

import android.content.Context
import androidx.annotation.StringRes

/**
 * Utility functions for accessing resources
 */
object ResourceUtils {
    /**
     * Get a string resource by its ID
     */
    fun getString(context: Context, @StringRes resId: Int): String {
        return context.getString(resId)
    }

    /**
     * Get a string resource with formatting arguments
     */
    fun getString(context: Context, @StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}