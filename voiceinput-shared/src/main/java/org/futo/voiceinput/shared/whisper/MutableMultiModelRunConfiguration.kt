package org.futo.voiceinput.shared.whisper

import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.ModelLoader

/**
 * Mutable version of MultiModelRunConfiguration to allow for dynamic model selection.
 */
class MutableMultiModelRunConfiguration(
    var primaryModel: ModelLoader,
    val languageSpecificModels: MutableMap<Language, ModelLoader>
) {
    /**
     * Convert to immutable MultiModelRunConfiguration.
     */
    fun toImmutable(): MultiModelRunConfiguration {
        return MultiModelRunConfiguration(
            primaryModel = primaryModel,
            languageSpecificModels = HashMap(languageSpecificModels)
        )
    }
    
    companion object {
        /**
         * Create a mutable configuration from an immutable one.
         */
        fun fromImmutable(config: MultiModelRunConfiguration): MutableMultiModelRunConfiguration {
            return MutableMultiModelRunConfiguration(
                primaryModel = config.primaryModel,
                languageSpecificModels = HashMap(config.languageSpecificModels)
            )
        }
    }
}
