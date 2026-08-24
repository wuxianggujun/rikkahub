package me.rerere.rikkahub.data.datastore

import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAvailabilityTest {
    @Test
    fun openAiRequiresKeyIncludingTheDefaultBuiltInProvider() {
        assertFalse(ProviderSetting.OpenAI().isAvailableForChat())
        assertTrue(ProviderSetting.OpenAI(apiKey = "key").isAvailableForChat())
        assertFalse(
            ProviderSetting.OpenAI(
                builtIn = true,
                baseUrl = "https://api.rikka-ai.com/v1",
            ).isAvailableForChat()
        )
    }

    @Test
    fun disabledProviderIsNeverAvailable() {
        assertFalse(
            ProviderSetting.OpenAI(
                enabled = false,
                apiKey = "key",
            ).isAvailableForChat()
        )
    }

    @Test
    fun googleServiceAccountRequiresAllCredentials() {
        val provider = ProviderSetting.Google(
            vertexAI = true,
            useServiceAccount = true,
            projectId = "project",
            serviceAccountEmail = "service@example.com",
            privateKey = "private-key",
        )
        assertTrue(provider.isAvailableForChat())
        assertFalse(provider.copy(privateKey = "").isAvailableForChat())
        assertTrue(
            provider.copy(vertexAI = false, useServiceAccount = false, apiKey = "key")
                .isAvailableForChat()
        )
    }

    @Test
    fun modelUsesItsProviderConfiguration() {
        val model = Model(modelId = "model")
        val provider = ProviderSetting.Claude(
            apiKey = "key",
            models = listOf(model),
        )
        assertTrue(model.isProviderAvailable(listOf(provider)))
        assertFalse(model.isProviderAvailable(listOf(provider.copy(apiKey = ""))))
    }
}
