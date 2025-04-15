package org.futo.voiceinput.shared.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.futo.voiceinput.shared.R
import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.ModelInferenceCallback
import org.futo.voiceinput.shared.types.ModelLoader
import org.futo.voiceinput.shared.util.NetworkUtils
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Example class demonstrating how to use remote Whisper processing.
 */
class WhisperRemoteProcessingExample(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "WhisperRemoteExample"
    
    /**
     * Process an audio file using Whisper.
     * This will use remote processing if enabled and available, otherwise it will fall back to local processing.
     *
     * @param audioFile The audio file to process
     * @param callback Callback for processing status and results
     */
    fun processAudioFile(audioFile: File, callback: (String) -> Unit) {
        coroutineScope.launch {
            try {
                // Load configuration
                val config = WhisperConfig.loadFromPreferences(context)
                
                // Use the createModelLoader method which handles all the checks
                // and updates the ProcessingModeTracker
                val localModel = getDefaultLocalModel()
                val modelLoader = config.createModelLoader(
                    context,
                    localModel,
                    R.string.remote_model_name
                )
                
                // Log the processing mode
                val isRemote = ProcessingModeTracker.isRemoteProcessing.value
                Log.d(TAG, "Processing mode: ${if (isRemote) "Remote" else "Local"}")
                
                // Create model manager
                val modelManager = ModelManager(context)
                
                
                // Get processor from model manager
                val processor = modelManager.obtainModel(modelLoader)
                
                try {
                    // Read audio file
                    val samples = readAudioFile(audioFile)
                    
                    // Process audio
                    val result = processor.process(
                        samples = samples,
                        prompt = "",
                        languages = arrayOf("en"),
                        bailLanguages = arrayOf(),
                        suppressNonSpeechTokens = true,
                        partialResultCallback = { partial ->
                            Log.d(TAG, "Partial result: $partial")
                            // Forward partial results to the callback
                            callback("Processing: $partial")
                        }
                    )
                
                    // Return result
                    withContext(Dispatchers.Main) {
                        callback(result)
                    }
                } finally {
                    // Clean up resources even if an exception occurs
                    try {
                        processor.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing processor", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                withContext(Dispatchers.Main) {
                    callback("Error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get the default local model.
     * This is just an example - in a real app, you would use your actual model.
     */
    private fun getDefaultLocalModel(): ModelLoader {
        // This is just a placeholder - in a real app, you would use your actual model
        return object : ModelLoader {
            override val name: Int = R.string.tiny_en_name
            
            override fun exists(context: Context): Boolean = true
            
            override fun getRequiredDownloadList(context: Context): List<String> = emptyList()
            
            override fun loadGGML(context: Context): WhisperProcessor {
                // This would load your actual model
                throw NotImplementedError("This is just an example")
            }
            
            override fun key(context: Context): Any = "default_local_model"
        }
    }
    
    /**
     * Read an audio file and convert it to float samples.
     * This is just an example - in a real app, you would use your actual audio processing.
     */
    private fun readAudioFile(file: File): FloatArray {
        // Read WAV file
        val fis = FileInputStream(file)
        val buffer = ByteArray(file.length().toInt())
        fis.read(buffer)
        fis.close()
        
        // Skip WAV header (44 bytes)
        val shortBuffer = ByteBuffer.wrap(buffer, 44, buffer.size - 44)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        
        // Convert to float array
        val samples = FloatArray(shortBuffer.remaining())
        for (i in samples.indices) {
            samples[i] = shortBuffer.get(i) / Short.MAX_VALUE.toFloat()
        }
        
        return samples
    }
}
