package org.futo.voiceinput.shared.whisper

/**
 * Interface for Whisper processing, allowing both local and remote implementations.
 */
interface WhisperProcessor {
    /**
     * Process audio samples and return transcription.
     *
     * @param samples The audio samples to process
     * @param prompt Initial prompt to guide the transcription
     * @param languages Array of allowed languages for detection
     * @param bailLanguages Languages that should trigger a bail-out
     * @param suppressNonSpeechTokens Whether to suppress non-speech tokens
     * @param partialResultCallback Callback for partial results during processing
     * @return The transcribed text
     */
    suspend fun process(
        samples: FloatArray,
        prompt: String,
        languages: Array<String>,
        bailLanguages: Array<String>,
        suppressNonSpeechTokens: Boolean,
        partialResultCallback: (String) -> Unit
    ): String
    
    /**
     * Cancel ongoing processing.
     */
    fun cancel()
    
    /**
     * Close and release resources.
     */
    suspend fun close()
}
