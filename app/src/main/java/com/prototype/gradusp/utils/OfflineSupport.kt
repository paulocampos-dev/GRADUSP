package com.prototype.gradusp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility class for detecting and managing offline/online connectivity status
 */
class OfflineSupport(private val context: Context) {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    enum class ConnectionType {
        NONE, WIFI, CELLULAR, ETHERNET, OTHER
    }

    /**
     * Check if device is currently online
     */
    fun checkConnectivity(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            val isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> ConnectionType.OTHER
                else -> ConnectionType.OTHER
            }

            _isOnline.value = isOnline
            _connectionType.value = if (isOnline) connectionType else ConnectionType.NONE

            return isOnline
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false

            val isOnline = networkInfo.isConnected
            _isOnline.value = isOnline
            _connectionType.value = if (isOnline) ConnectionType.OTHER else ConnectionType.NONE

            return isOnline
        }
    }

    /**
     * Get user-friendly connection status message
     */
    fun getConnectionStatusMessage(): String {
        return when {
            !_isOnline.value -> "Sem conexão com a internet"
            _connectionType.value == ConnectionType.WIFI -> "Conectado via Wi-Fi"
            _connectionType.value == ConnectionType.CELLULAR -> "Conectado via dados móveis"
            _connectionType.value == ConnectionType.ETHERNET -> "Conectado via Ethernet"
            else -> "Conectado"
        }
    }

    /**
     * Check if connection is suitable for large data transfers
     */
    fun isConnectionSuitableForLargeData(): Boolean {
        return _isOnline.value && (_connectionType.value == ConnectionType.WIFI || _connectionType.value == ConnectionType.ETHERNET)
    }

    /**
     * Force refresh connectivity status
     */
    fun refreshConnectivity() {
        checkConnectivity()
    }

    companion object {
        /**
         * Check if device has internet connectivity (one-time check)
         */
        fun isDeviceOnline(context: Context): Boolean {
            val offlineSupport = OfflineSupport(context)
            return offlineSupport.checkConnectivity()
        }
    }
}
