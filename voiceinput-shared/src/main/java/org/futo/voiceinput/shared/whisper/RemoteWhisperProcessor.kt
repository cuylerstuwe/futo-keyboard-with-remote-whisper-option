package org.futo.voiceinput.shared.whisper

import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.futo.voiceinput.shared.ggml.BailLanguageException
import org.futo.voiceinput.shared.ggml.InferenceCancelledException
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "RemoteWhisperProcessor"

/**
 * Implementation of WhisperProcessor that sends audio to a remote server for processing.
 */
class RemoteWhisperProcessor(
    private val serverUrl: String,
    private val apiKey: String? = null,
    private val modelName: String = "whisper-large-v3"
) : WhisperProcessor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private var currentCall: Call? = null
    private var isCancelled = false
    
    /**
     * Converts float audio samples to WAV format.
     */
    private fun floatArrayToWav(samples: FloatArray): ByteArray {
        // Estimate the size to avoid reallocations
        val estimatedSize = 44 + (samples.size * 2) // 44 bytes header + 2 bytes per sample
        val outputStream = ByteArrayOutputStream(estimatedSize)
        
        // WAV header
        val sampleRate = 16000
        val numChannels = 1
        val bitsPerSample = 16
        val dataSize = samples.size * (bitsPerSample / 8)
        val totalSize = 36 + dataSize
        
        // RIFF header
        outputStream.write("RIFF".toByteArray())
        outputStream.write(intToByteArray(totalSize))
        outputStream.write("WAVE".toByteArray())
        
        // Format chunk
        outputStream.write("fmt ".toByteArray())
        outputStream.write(intToByteArray(16)) // Chunk size
        outputStream.write(shortToByteArray(1)) // Audio format (PCM)
        outputStream.write(shortToByteArray(numChannels.toShort()))
        outputStream.write(intToByteArray(sampleRate))
        outputStream.write(intToByteArray(sampleRate * numChannels * (bitsPerSample / 8))) // Byte rate
        outputStream.write(shortToByteArray((numChannels * (bitsPerSample / 8)).toShort())) // Block align
        outputStream.write(shortToByteArray(bitsPerSample.toShort()))
        
        // Data chunk
        outputStream.write("data".toByteArray())
        outputStream.write(intToByteArray(dataSize))
        
        // Audio data
        for (sample in samples) {
            // Convert float to short (16-bit PCM)
            val shortSample = (sample * Short.MAX_VALUE).toInt().toShort()
            outputStream.write(shortToByteArray(shortSample))
        }
        
        return outputStream.toByteArray()
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value shr 8).toByte(),
            (value shr 16).toByte(),
            (value shr 24).toByte()
        )
    }
    
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value.toInt() shr 8).toByte()
        )
    }
    
    override suspend fun process(
        samples: FloatArray,
        prompt: String,
        languages: Array<String>,
        bailLanguages: Array<String>,
        suppressNonSpeechTokens: Boolean,
        partialResultCallback: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        // Reset the cancelled flag at the beginning of each process call
        isCancelled = false
        
        if (isCancelled) {
            throw InferenceCancelledException()
        }
        
        // Send initial partial result to indicate processing has started
        withContext(Dispatchers.Main) {
            partialResultCallback("Processing on remote server...")
        }
        
        // Convert audio samples to WAV format
        val audioData = try {
            floatArrayToWav(samples)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting audio to WAV", e)
            throw IOException("Error preparing audio data: ${e.message}")
        }
        
        // Create multipart request
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "audio.wav",
                audioData.toRequestBody("audio/wav".toMediaType())
            )
            .addFormDataPart("model", modelName)
        
        // Add prompt if provided
        if (prompt.isNotEmpty()) {
            requestBody.addFormDataPart("prompt", prompt)
        }
        
        // Add language if only one is specified
        if (languages.size == 1) {
            requestBody.addFormDataPart("language", languages[0])
        }
        
        // Build the request
        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody.build())
        
        // Add API key if provided
        if (apiKey != null && apiKey.isNotBlank()) {
            request.addHeader("Authorization", "Bearer $apiKey")
            Log.d(TAG, "Using API key: ${apiKey.take(4)}...${apiKey.takeLast(4)}")
        } else {
            Log.w(TAG, "No API key provided or key is blank")
        }
        
        // Execute the request
        return@withContext suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request.build())
            currentCall = call
            
            continuation.invokeOnCancellation {
                call.cancel()
            }
            
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "API request failed", e)
                    if (isCancelled) {
                        continuation.resumeWithException(InferenceCancelledException())
                    } else {
                        // Provide more specific error message based on the exception
                        val errorMessage = when {
                            e.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Connection timed out. The server took too long to respond."
                            e.message?.contains("refused", ignoreCase = true) == true -> 
                                "Connection refused. The server may be down or the URL is incorrect."
                            e.message?.contains("unable to resolve", ignoreCase = true) == true -> 
                                "Unable to resolve host. Check your internet connection and server URL."
                            else -> "Network error: ${e.message}"
                        }
                        continuation.resumeWithException(IOException(errorMessage))
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.e(TAG, "API error: ${response.code} - $errorBody")
                        
                        // Provide more specific error messages for common HTTP errors
                        val errorMessage = when (response.code) {
                            401 -> "Authentication failed. Please check your API key."
                            403 -> "Access forbidden. Your API key may not have permission to use this service."
                            404 -> "API endpoint not found. Please check the server URL."
                            429 -> "Too many requests. You may have exceeded your API rate limit."
                            500, 502, 503, 504 -> "Server error (${response.code}). The remote service is experiencing issues."
                            else -> "API error: ${response.code} - $errorBody"
                        }
                        
                        continuation.resumeWithException(IOException(errorMessage))
                        return
                    }
                    
                    try {
                        val responseBody = response.body?.string()
                        if (responseBody == null) {
                            continuation.resumeWithException(IOException("Empty response"))
                            return
                        }
                        
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val text = jsonResponse.optString("text", "")
                            
                            // Check if the detected language is in the bail languages
                            val detectedLanguage = jsonResponse.optString("language", "")
                            if (detectedLanguage.isNotEmpty() && bailLanguages.contains(detectedLanguage)) {
                                continuation.resumeWithException(BailLanguageException(detectedLanguage))
                                return
                            }
                            
                            // Send final partial result before completing
                            partialResultCallback(text)
                            
                            continuation.resume(text)
                        } catch (e: JSONException) {
                            Log.e(TAG, "Error parsing JSON response", e)
                            continuation.resumeWithException(IOException("Invalid response from server: ${e.message}"))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }
    
    override fun cancel() {
        isCancelled = true
        currentCall?.cancel()
    }
    
    override suspend fun close() {
        // Cancel any ongoing requests
        cancel()
        
        // Close the OkHttp client to release resources
        withContext(Dispatchers.IO) {
            try {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing OkHttp client", e)
            }
        }
    }
}
