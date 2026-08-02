package io.github.miksask.linkagram.data.extract

fun interface MetaCapturePolicy {
    fun shouldCapture(host: String?): Boolean
}
