package com.ravi.busmanagementt.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkConnectivityManager {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        // Emit initial state immediately
        trySend(if (isNetworkAvailable()) NetworkStatus.Available else NetworkStatus.Unavailable)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network is available, but we should check if it has internet capabilities
                // For simple available check, we can emit Available here,
                // but usually CapabilitiesChanged is where you verify 'VALIDATED'
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                // On newer Androids, NET_CAPABILITY_VALIDATED tells us if the system actually
                // successfully pinged the real internet (avoiding captive portals/no-internet wifi).
                val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                } else {
                    true // Assume true on older devices if INTERNET cap exists
                }

                if (hasInternet && isValidated) {
                    trySend(NetworkStatus.Available)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ (API 24) - Best for tracking the active default network
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            // Older Android versions
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    override fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                } else {
                    true
                }
    }
}

interface NetworkConnectivityManager {
    val networkStatus: Flow<NetworkStatus>
    fun isNetworkAvailable(): Boolean
}

enum class NetworkStatus {
    Available, Unavailable
}