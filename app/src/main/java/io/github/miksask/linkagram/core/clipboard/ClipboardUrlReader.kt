package io.github.miksask.linkagram.core.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context

class ClipboardUrlReader(
    private val clipboardManager: ClipboardManager,
) {
    constructor(context: Context) : this(
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager,
    )

    fun readText(): String? {
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        if (!clipboardManager.primaryClipDescription.hasTextMime()) return null
        return clip.getItemAt(0).coerceToText(null)?.toString()?.trim()?.ifEmpty { null }
    }

    private fun ClipDescription?.hasTextMime(): Boolean {
        if (this == null) return false
        return hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) ||
            hasMimeType("text/*")
    }
}
