package com.example.eventassistant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Checks whether the phone currently has a network connection that can reach the internet. */
public final class NetworkUtils {

    private NetworkUtils() { }

    /**
     * True if the active network (Wi-Fi, mobile data, ...) offers internet access.
     * Needs the ACCESS_NETWORK_STATE permission.
     *
     * This is only a quick pre-check so the app can say "no internet" immediately. A network
     * that claims internet access can still fail (weak signal, blocked server), so the actual
     * upload also handles connection errors and time-outs.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();           // null when there is no connection
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
