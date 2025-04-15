package org.futo.voiceinput.shared.whisper

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ProcessingModeTracker"

/**
 * Singleton to track whether the current processing is happening locally or remotely.
 */
object ProcessingModeTracker {
    private val _isRemoteProcessing = MutableStateFlow(false)
    val isRemoteProcessing: StateFlow<Boolean> = _isRemoteProcessing.asStateFlow()
    
    // Track whether remote processing is enabled in settings
    private val _isRemoteEnabled = MutableStateFlow(false)
    val isRemoteEnabled: StateFlow<Boolean> = _isRemoteEnabled.asStateFlow()
    
    /**
     * Set the current processing mode.
     * 
     * @param isRemote true if processing is happening remotely, false if locally
     */
    fun setRemoteProcessing(isRemote: Boolean) {
        Log.d(TAG, "Setting remote processing mode: $isRemote")
        _isRemoteProcessing.value = isRemote
    }
    
    /**
     * Set whether remote processing is enabled in settings.
     * 
     * @param isEnabled true if remote processing is enabled in settings
     */
    fun setRemoteEnabled(isEnabled: Boolean) {
        Log.d(TAG, "Setting remote enabled: $isEnabled")
        _isRemoteEnabled.value = isEnabled
    }
    
    /**
     * Composable function to get the current processing mode as a State.
     */
    @Composable
    fun rememberIsRemoteProcessing(): State<Boolean> {
        val context = LocalContext.current
        val config = remember { WhisperConfig.loadFromPreferences(context) }
        
        // Update the enabled state based on config
        // This should be done in a side effect to avoid recomposition issues
        androidx.compose.runtime.LaunchedEffect(config.useRemoteProcessing) {
            setRemoteEnabled(config.useRemoteProcessing)
        }
        
        // Collect the remote processing state as a composable state
        return isRemoteProcessing.collectAsState(initial = false)
    }
}
