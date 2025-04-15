package org.futo.voiceinput.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.futo.voiceinput.shared.R
import org.futo.voiceinput.shared.whisper.ProcessingModeTracker
import org.futo.voiceinput.shared.whisper.WhisperConfig

/**
 * Example settings screen for Whisper voice processing.
 * This demonstrates how to integrate the WhisperSettingsView into a settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhisperSettingsScreen() {
    val context = LocalContext.current
    val config = remember { WhisperConfig.loadFromPreferences(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remote_processing_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                scrollBehavior = null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Voice Processing Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add the WhisperSettingsView
            WhisperSettingsView(
                initialConfig = config,
                onConfigChanged = { newConfig ->
                    // Handle config changes if needed
                    // For example, you might want to update some UI state
                    // or trigger a reload of the models
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add a processing indicator example
            Text(
                text = "Processing Indicator Example:",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show the processing indicator based on the current config and processing state
            val isRemoteProcessing = ProcessingModeTracker.rememberIsRemoteProcessing()
            ProcessingIndicator(
                isRemoteProcessing = isRemoteProcessing.value
            )
        }
    }
}
