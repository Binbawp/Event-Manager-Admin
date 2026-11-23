package com.example.eventassistant;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences manager for tracking local database modifications and cloud synchronization state.
 * <p>
 * Pending local changes are tracked using two counters:
 * <ul>
 *     <li>{@code changeCounter}: incremented on every local insert, update, or delete operation.</li>
 *     <li>{@code syncedCounter}: stores the snapshot of {@code changeCounter} covered by the last successful upload.</li>
 * </ul>
 * Pending changes exist whenever {@code changeCounter > syncedCounter}.
 */
public final class SyncPrefs {

    private static final String FILE_NAME = "sync_prefs";
    private static final String KEY_CHANGE_COUNTER = "change_counter";
    private static final String KEY_SYNCED_COUNTER = "synced_counter";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_AUTO_SYNC = "auto_sync";

    private SyncPrefs() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Increments the database change counter.
     * Called by {@link DatabaseHelper} whenever an event or session is created, modified, or removed.
     *
     * @param context the application context
     */
    public static synchronized void recordChange(Context context) {
        SharedPreferences p = prefs(context);
        p.edit().putLong(KEY_CHANGE_COUNTER, p.getLong(KEY_CHANGE_COUNTER, 0) + 1).apply();
    }

    /**
     * Retrieves the current change counter value.
     *
     * @param context the application context
     * @return total change count
     */
    public static long getChangeCounter(Context context) {
        return prefs(context).getLong(KEY_CHANGE_COUNTER, 0);
    }

    /**
     * Updates sync state counters and timestamp following a successful cloud payload upload.
     *
     * @param context         the application context
     * @param counterAtUpload change counter value when the uploaded snapshot was captured
     * @param timeMillis      epoch timestamp of successful synchronization
     */
    public static synchronized void markSynced(Context context, long counterAtUpload, long timeMillis) {
        prefs(context).edit()
                .putLong(KEY_SYNCED_COUNTER, counterAtUpload)
                .putLong(KEY_LAST_SYNC_TIME, timeMillis)
                .apply();
    }

    /**
     * Checks if local database modifications exist that have not yet been synchronized to the cloud.
     *
     * @param context the application context
     * @return true if local changes are pending synchronization; false otherwise
     */
    public static boolean hasPendingChanges(Context context) {
        SharedPreferences p = prefs(context);
        return p.getLong(KEY_CHANGE_COUNTER, 0) > p.getLong(KEY_SYNCED_COUNTER, 0);
    }

    /**
     * Retrieves the timestamp of the last successful cloud upload.
     *
     * @param context the application context
     * @return last sync timestamp in milliseconds, or 0 if never synced
     */
    public static long getLastSyncTime(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC_TIME, 0);
    }

    /**
     * Checks whether a cloud sync has been successfully completed at least once.
     *
     * @param context the application context
     * @return true if previous sync history exists; false otherwise
     */
    public static boolean hasEverSynced(Context context) {
        return getLastSyncTime(context) > 0;
    }

    /**
     * Checks if automatic cloud synchronization is enabled.
     *
     * @param context the application context
     * @return true if auto-sync is enabled; false otherwise
     */
    public static boolean isAutoSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SYNC, true);
    }

    /**
     * Enables or disables automatic cloud synchronization setting.
     *
     * @param context the application context
     * @param enabled desired auto-sync state
     */
    public static void setAutoSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }
}
