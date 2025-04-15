package org.futo.voiceinput.shared.whisper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.futo.voiceinput.shared.types.ModelLoader
import org.futo.voiceinput.shared.types.RemoteModelLoader
import org.futo.voiceinput.shared.util.NetworkUtils

private const val TAG = "WhisperConfig"


/**
 * Configuration for Whisper processing.
 */
data class WhisperConfig(
    val useRemoteProcessing: Boolean = false,
    val remoteServerUrl: String = "https://api.openai.com/v1/audio/transcriptions",
    val apiKey: String? = null,
    val remoteModelName: String = "whisper-1"
) {
    companion object {
        private const val PREF_USE_REMOTE = "whisper_use_remote"
        private const val PREF_REMOTE_URL = "whisper_remote_url"
        private const val PREF_API_KEY = "whisper_api_key"
        private const val PREF_REMOTE_MODEL = "whisper_remote_model"
        
        /**
         * Load configuration from SharedPreferences.
         */
        fun loadFromPreferences(context: Context): WhisperConfig {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return WhisperConfig(
                useRemoteProcessing = prefs.getBoolean(PREF_USE_REMOTE, false),
                remoteServerUrl = prefs.getString(PREF_REMOTE_URL, "https://api.openai.com/v1/audio/transcriptions") ?: "https://api.openai.com/v1/audio/transcriptions",
                apiKey = prefs.getString(PREF_API_KEY, null),
                remoteModelName = prefs.getString(PREF_REMOTE_MODEL, "whisper-1") ?: "whisper-1"
            )
        }
        
        /**
         * Save configuration to SharedPreferences.
         */
        fun saveToPreferences(context: Context, config: WhisperConfig) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().apply {
                putBoolean(PREF_USE_REMOTE, config.useRemoteProcessing)
                putString(PREF_REMOTE_URL, config.remoteServerUrl)
                
                // Always save the API key, even if it's empty
                // This ensures we don't use a stale key if the user clears it
                putString(PREF_API_KEY, config.apiKey ?: "")
                
                putString(PREF_REMOTE_MODEL, config.remoteModelName)
            }.apply()
            
            // Update the ProcessingModeTracker
            ProcessingModeTracker.setRemoteEnabled(config.useRemoteProcessing)
        }
    }
    
    /**
     * Create a ModelLoader based on the configuration.
     * If useRemoteProcessing is true and network is available, returns a RemoteModelLoader.
     * Otherwise, returns the provided localModel.
     */
    fun createModelLoader(context: Context, localModel: ModelLoader, nameResId: Int): ModelLoader {
        // Check if remote processing is enabled
        if (!useRemoteProcessing) {
            Log.d(TAG, "Remote processing disabled in settings")
            ProcessingModeTracker.setRemoteProcessing(false)
            return localModel
        }
        
        // Initialize NetworkUtils if not already initialized
        NetworkUtils.initialize(context)
        
        // Check if network is available
        val networkAvailable = NetworkUtils.isNetworkAvailable(context)
        if (!networkAvailable) {
            Log.w(TAG, "Network not available, falling back to local processing")
            ProcessingModeTracker.setRemoteProcessing(false)
            return localModel
        }
        
        // Check if API key is provided if needed
        if (remoteServerUrl.contains("openai.com") && apiKey.isNullOrBlank()) {
            Log.w(TAG, "API key not provided for OpenAI API, falling back to local processing")
            ProcessingModeTracker.setRemoteProcessing(false)
            return localModel
        }
        
        // Use remote processing
        Log.d(TAG, "Using remote processing with model: $remoteModelName")
        ProcessingModeTracker.setRemoteProcessing(true)
        return RemoteModelLoader(
            name = nameResId,
            serverUrl = remoteServerUrl,
            apiKey = apiKey,
            modelName = remoteModelName
        )
    }
}
