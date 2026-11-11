package com.example.eventassistant;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Small settings store (SharedPreferences, Lecture 5) that remembers the cloud sync state.
 *
 * "Pending changes" is tracked with two counters instead of a true/false flag:
 *   - changeCounter goes up every time the local database changes
 *   - syncedCounter is the changeCounter value that the last successful upload covered
 * Changes are pending while changeCounter > syncedCounter. This stays correct even if the
 * admin edits an event while an upload is still running (a plain flag would wrongly be
 * cleared when that upload finishes).
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

    /** Called by DatabaseHelper whenever an event or session is added, edited or deleted. */
    public static synchronized void recordChange(Context context) {
        SharedPreferences p = prefs(context);
        p.edit().putLong(KEY_CHANGE_COUNTER, p.getLong(KEY_CHANGE_COUNTER, 0) + 1).apply();
    }

    public static long getChangeCounter(Context context) {
        return prefs(context).getLong(KEY_CHANGE_COUNTER, 0);
    }

    /** Called after a successful upload with the change counter value the upload was built from. */
    public static synchronized void markSynced(Context context, long counterAtUpload, long timeMillis) {
        prefs(context).edit()
                .putLong(KEY_SYNCED_COUNTER, counterAtUpload)
                .putLong(KEY_LAST_SYNC_TIME, timeMillis)
                .apply();
    }

    public static boolean hasPendingChanges(Context context) {
        SharedPreferences p = prefs(context);
        return p.getLong(KEY_CHANGE_COUNTER, 0) > p.getLong(KEY_SYNCED_COUNTER, 0);
    }

    public static long getLastSyncTime(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC_TIME, 0);
    }

    /** Automatic syncing only starts after the admin has done one manual upload. */
    public static boolean hasEverSynced(Context context) {
        return getLastSyncTime(context) > 0;
    }

    public static boolean isAutoSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SYNC, true);
    }

    public static void setAutoSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }
}
