package org.futo.voiceinput.shared.whisper

import android.content.Context
import org.futo.voiceinput.shared.types.ModelLoader


class ModelManager(
    val context: Context
) {
    private val loadedModels: HashMap<Any, WhisperProcessor> = hashMapOf()

    fun obtainModel(model: ModelLoader): WhisperProcessor {
        val key = model.key(context)
        if (!loadedModels.contains(key)) {
            loadedModels[key] = model.loadGGML(context)
        }

        return loadedModels[key]!!
    }

    fun cancelAll() {
        loadedModels.forEach {
            it.value.cancel()
        }
    }

    suspend fun cleanUp() {
        for (model in loadedModels.entries) {
            model.value.close()
        }

        loadedModels.clear()
    }
}
