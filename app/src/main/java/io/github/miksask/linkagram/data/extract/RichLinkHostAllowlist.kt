package io.github.miksask.linkagram.data.extract

object RichLinkHostAllowlist : MetaCapturePolicy {
    override fun shouldCapture(host: String?): Boolean {
        val normalized = host?.lowercase().orEmpty()
        if (normalized.isEmpty()) return false
        return normalized == "koleo.pl" || normalized.endsWith(".koleo.pl")
    }
}
