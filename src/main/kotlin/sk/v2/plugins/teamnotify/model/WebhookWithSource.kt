package sk.v2.plugins.teamnotify.model

data class WebhookWithSource(
    val webhook: WebhookConfiguration,
    val source: WebhookSource,
    val isLocallyDisabled: Boolean = false
)

enum class WebhookSource {
    BUILD_TYPE,  // Defined directly on the build configuration
    PROJECT,     // Inherited from project
    DSL          // Defined in versioned settings/DSL
}

data class BuildTypeDisabledWebhooks(
    val disabledWebhookUrls: Set<String> = emptySet()
)