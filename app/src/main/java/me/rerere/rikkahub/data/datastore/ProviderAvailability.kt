package me.rerere.rikkahub.data.datastore

import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting

fun Model.isProviderAvailable(providers: List<ProviderSetting>): Boolean =
    findProvider(providers)?.isAvailableForChat() == true

fun ProviderSetting.isAvailableForChat(): Boolean {
    if (!enabled) return false

    return when (this) {
        is ProviderSetting.OpenAI -> apiKey.isNotBlank()
        is ProviderSetting.Claude -> apiKey.isNotBlank()
        is ProviderSetting.Google -> {
            if (vertexAI && useServiceAccount) {
                projectId.isNotBlank() &&
                    serviceAccountEmail.isNotBlank() &&
                    privateKey.isNotBlank()
            } else {
                apiKey.isNotBlank()
            }
        }
    }
}
