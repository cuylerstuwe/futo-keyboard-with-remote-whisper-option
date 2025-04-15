package org.futo.voiceinput.shared.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "NetworkUtils"

/**
 * Utility class for network-related operations.
 */
object NetworkUtils {
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private var isInitialized = false
    
    // Store the network callback to prevent it from being garbage collected
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    /**
     * Initialize network monitoring.
     * This should be called from the application's onCreate method.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Set initial state
        _isNetworkAvailable.value = checkNetworkAvailability(connectivityManager)
        
        // Register network callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available")
                    _isNetworkAvailable.value = true
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    _isNetworkAvailable.value = checkNetworkAvailability(connectivityManager)
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    Log.d(TAG, "Network capabilities changed, has internet: $hasInternet")
                    _isNetworkAvailable.value = hasInternet
                }
            }
            
            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering network callback", e)
                // Fall back to simple check
                _isNetworkAvailable.value = checkNetworkAvailability(connectivityManager)
            }
        }
        
        isInitialized = true
    }
    
    /**
     * Check if the device has an active internet connection.
     *
     * @param context The application context
     * @return true if the device has an active internet connection, false otherwise
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // Initialize if not already initialized
        if (!isInitialized) {
            initialize(context)
        }
        
        return _isNetworkAvailable.value
    }
    
    /**
     * Check network availability using the ConnectivityManager.
     */
    private fun checkNetworkAvailability(connectivityManager: ConnectivityManager): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
