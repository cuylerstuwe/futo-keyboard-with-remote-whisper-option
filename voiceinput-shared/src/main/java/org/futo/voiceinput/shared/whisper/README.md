# Remote Whisper Processing

This implementation adds support for remote Whisper voice processing, allowing the use of larger Whisper models that would be impractical to run on a mobile device due to performance and battery constraints.

## Overview

The implementation provides:

1. A flexible architecture that supports both local and remote processing
2. Automatic fallback to local processing when network is unavailable
3. User interface for configuring remote processing settings
4. Visual indicator showing whether processing is happening locally or remotely

## Architecture

The key components of the implementation are:

### Core Components

- **WhisperProcessor**: Interface that abstracts the processing logic, allowing both local and remote implementations
- **WhisperGGML**: Local implementation of WhisperProcessor (existing code, updated to implement the interface)
- **RemoteWhisperProcessor**: Remote implementation of WhisperProcessor that sends audio to a remote server
- **WhisperConfig**: Configuration class for remote processing settings
- **ProcessingModeTracker**: Singleton to track whether processing is happening locally or remotely
- **MutableMultiModelRunConfiguration**: Helper class for dynamic model selection

### UI Components

- **WhisperSettingsView**: Composable for configuring remote processing settings
- **ProcessingIndicator**: Composable for showing whether processing is happening locally or remotely

### Utility Components

- **NetworkUtils**: Utility class for checking network availability and monitoring network state changes
- **RemoteModelLoader**: Model loader for remote processing

## How It Works

1. The user configures remote processing settings in the UI
2. When voice recognition is triggered, the system:
   - Checks if remote processing is enabled
   - Checks if network is available using the NetworkUtils (which continuously monitors network state)
   - Checks if API key is provided (if needed)
   - If all checks pass, uses remote processing; otherwise, falls back to local processing
3. The UI shows whether processing is happening locally or remotely
4. The audio is processed either locally or remotely, and the results are returned to the user
5. If network conditions change during processing, the system can adapt accordingly

## Remote API Requirements

The remote API should:

1. Accept audio data in WAV format
2. Accept a model name parameter
3. Accept an optional prompt parameter
4. Accept an optional language parameter
5. Return a JSON response with a "text" field containing the transcription
6. Optionally return a "language" field with the detected language

## Configuration Options

The following configuration options are available:

- **Use Remote Processing**: Enable/disable remote processing
- **Server URL**: URL of the remote API server
- **API Key**: API key for authentication (if required)
- **Model Name**: Name of the model to use on the remote server

## Fallback Mechanism

The system will automatically fall back to local processing if:

1. Remote processing is disabled
2. Network is unavailable (detected by the NetworkUtils monitoring)
3. API key is not provided (if required)
4. Remote server returns an error

## Initialization

To ensure proper network monitoring, initialize the NetworkUtils in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize NetworkUtils for network monitoring
        NetworkUtils.initialize(applicationContext)
    }
}
```

## Usage

### Adding to Settings

To add the remote processing settings to your settings screen:

```kotlin
@Composable
fun SettingsScreen() {
    val config = remember { WhisperConfig.loadFromPreferences(LocalContext.current) }
    
    WhisperSettingsView(
        initialConfig = config,
        onConfigChanged = { newConfig ->
            // Handle config changes if needed
        }
    )
}
```

### Showing Processing Indicator

To show the processing indicator in your UI:

```kotlin
@Composable
fun RecognitionScreen() {
    val isRemoteProcessing = ProcessingModeTracker.rememberIsRemoteProcessing()
    
    ProcessingIndicator(
        isRemoteProcessing = isRemoteProcessing.value,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}
```

### Using Remote Processing in Code

To use remote processing in your code:

```kotlin
// Load configuration
val config = WhisperConfig.loadFromPreferences(context)

// Create model loader with remote processing if enabled
val modelLoader = config.createModelLoader(
    context,
    localModel,
    R.string.remote_model_name
)

// Use the model loader as usual
val processor = modelManager.obtainModel(modelLoader)
```

## Benefits

- **Better Accuracy**: Access to larger, more accurate models
- **Reduced Battery Usage**: Processing happens on a remote server, saving battery
- **Reduced Memory Usage**: No need to load large models into memory
- **Flexibility**: Can switch between local and remote processing based on network availability
