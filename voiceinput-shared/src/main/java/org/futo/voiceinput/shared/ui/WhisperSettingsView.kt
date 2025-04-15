package org.futo.voiceinput.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.futo.voiceinput.shared.R
import org.futo.voiceinput.shared.whisper.ProcessingModeTracker
import org.futo.voiceinput.shared.whisper.WhisperConfig

/**
 * Composable for configuring Whisper settings.
 */
@Composable
fun WhisperSettingsView(
    initialConfig: WhisperConfig = WhisperConfig(),
    onConfigChanged: (WhisperConfig) -> Unit = {}
) {
    val context = LocalContext.current
    
    var config by remember { mutableStateOf(initialConfig) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.remote_processing_settings),
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle for remote processing
            Column {
                Text(
                    text = stringResource(R.string.use_remote_processing),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.remote_processing_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.useRemoteProcessing,
                    onCheckedChange = { newValue -> 
                        val newConfig = config.copy(useRemoteProcessing = newValue)
                        config = newConfig
                        onConfigChanged(newConfig)
                        WhisperConfig.saveToPreferences(context, newConfig)
                        
                        // Update the ProcessingModeTracker
                        ProcessingModeTracker.setRemoteEnabled(newValue)
                    }
                )
            }
            
            if (config.useRemoteProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Server URL
                OutlinedTextField(
                    value = config.remoteServerUrl,
                    onValueChange = { newValue -> 
                        val newConfig = config.copy(remoteServerUrl = newValue)
                        config = newConfig
                        onConfigChanged(newConfig)
                        WhisperConfig.saveToPreferences(context, newConfig)
                    },
                    label = { Text(stringResource(R.string.server_url)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // API Key
                OutlinedTextField(
                    value = config.apiKey ?: "",
                    onValueChange = { newValue -> 
                        // Always save the API key, even if it's empty
                        val newConfig = config.copy(apiKey = newValue)
                        config = newConfig
                        onConfigChanged(newConfig)
                        WhisperConfig.saveToPreferences(context, newConfig)
                    },
                    label = { Text(stringResource(R.string.api_key)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Model Name
                OutlinedTextField(
                    value = config.remoteModelName,
                    onValueChange = { newValue -> 
                        val newConfig = config.copy(remoteModelName = newValue)
                        config = newConfig
                        onConfigChanged(newConfig)
                        WhisperConfig.saveToPreferences(context, newConfig)
                    },
                    label = { Text(stringResource(R.string.model_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.remote_model_name),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
