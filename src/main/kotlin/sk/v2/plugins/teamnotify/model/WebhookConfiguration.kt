package sk.v2.plugins.teamnotify.model

enum class WebhookPlatform {
    SLACK,
    TEAMS,
    DISCORD
}

data class WebhookConfiguration(
    val url: String,
    val platform: WebhookPlatform,
    val onStart: Boolean = false,
    val onSuccess: Boolean = false,
    val onFailure: Boolean = false,
    val onStall: Boolean = false,
    val onCancel: Boolean = false,  // New trigger for cancelled builds
    val buildLongerThan: Int? = null,
    val buildLongerThanAverage: Boolean = false,
    val onFirstFailure: Boolean = false,
    val onBuildFixed: Boolean = false,
    val includeChanges: Boolean = true,  // Default to true for backward compatibility
    val showBuildLink: Boolean = true,   // Show "Open in TeamCity" link
    val showArtifacts: Boolean = true,   // Show artifacts section
    val branchFilter: String? = null,  // Branch filter pattern (e.g., "+:main,+:release/*,-:feature/*")
    val authHeaderName: String? = null,  // Optional auth header name (e.g., "Authorization")
    val authHeaderValue: String? = null,  // Optional auth header value (e.g., "Bearer token")
    val enabled: Boolean = true
)