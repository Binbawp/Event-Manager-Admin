package com.example.eventassistant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Utility class for verifying network connectivity and internet availability.
 */
public final class NetworkUtils {

    private NetworkUtils() { }

    /**
     * Checks if the active network connection provides internet capability.
     * Requires the {@code android.permission.ACCESS_NETWORK_STATE} permission in AndroidManifest.xml.
     *
     * @param context the application context
     * @return true if an active network connection with internet capability is available; false otherwise
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();           // Returns null if no network is active
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
