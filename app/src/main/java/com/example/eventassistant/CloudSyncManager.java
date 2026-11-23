package com.example.eventassistant;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages asynchronous cloud synchronization of local event and session data to Firebase Realtime Database.
 * <p>
 * Payload synchronization strategy:
 * <ul>
 *     <li>The local database records are serialized into a consolidated JSON payload.</li>
 *     <li>An HTTP {@code PUT} request is issued to replace the cloud database node at {@code /events.json}.</li>
 *     <li>Network calls are executed off the UI thread via a single-threaded executor pool to prevent UI blocking.</li>
 *     <li>Sync results and callbacks are dispatched back to the main UI thread via {@link Handler}.</li>
 * </ul>
 * Cloud JSON structure format:
 * <pre>
 *   events / {eventCloudId} / name, date, location, organiser, description, contactEmail, contactPhone,
 *                             sessions / s{sessionId} / name, time, speaker, room, type, description
 * </pre>
 */
public class CloudSyncManager {

    /** Base URL endpoint for Firebase Realtime Database. */
    private static final String FIREBASE_URL =
            "https://event-manager-ac75b-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String EVENTS_URL = FIREBASE_URL + "/events.json";
    private static final int TIMEOUT_MS = 15000;

    /** Single-threaded executor ensuring sequential background upload tasks. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean UPLOAD_RUNNING = new AtomicBoolean(false);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Callback interface for delivering synchronization status results to caller components.
     */
    public interface SyncCallback {
        /**
         * Invoked on main thread when synchronization completes successfully.
         *
         * @param eventCount   total number of events uploaded
         * @param sessionCount total number of sessions uploaded
         */
        void onSuccess(int eventCount, int sessionCount);

        /**
         * Invoked on main thread when synchronization fails.
         *
         * @param message localized user-friendly error message
         */
        void onError(String message);
    }

    private final Context appContext;

    public CloudSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Initiates asynchronous cloud synchronization. Must be invoked from the main UI thread.
     *
     * @param callback callback to handle success or error responses
     */
    public void syncNow(SyncCallback callback) {
        if (!NetworkUtils.isOnline(appContext)) {
            callback.onError(appContext.getString(R.string.error_offline));
            return;
        }
        if (!UPLOAD_RUNNING.compareAndSet(false, true)) {
            callback.onError(appContext.getString(R.string.error_sync_busy));
            return;
        }

        // Capture local change counter state prior to initiating upload payload construction
        final long counterAtStart = SyncPrefs.getChangeCounter(appContext);

        EXECUTOR.execute(() -> {
            DatabaseHelper db = new DatabaseHelper(appContext);
            try {
                List<Event> events = db.getEvents(null);

                // Prevent initial empty database sync from wiping cloud data on new installation
                if (events.isEmpty() && !SyncPrefs.hasEverSynced(appContext)) {
                    postError(callback, appContext.getString(R.string.error_nothing_to_upload));
                    return;
                }

                Payload payload = buildPayload(db, events);
                putJson(payload.json);

                // Mark events as synced if no local modifications occurred during upload window
                if (SyncPrefs.getChangeCounter(appContext) == counterAtStart) {
                    db.markAllEventsSynced();
                }
                SyncPrefs.markSynced(appContext, counterAtStart, System.currentTimeMillis());

                final int eventCount = payload.eventCount;
                final int sessionCount = payload.sessionCount;
                MAIN_HANDLER.post(() -> callback.onSuccess(eventCount, sessionCount));
            } catch (SocketTimeoutException e) {
                postError(callback, appContext.getString(R.string.error_sync_failed,
                        appContext.getString(R.string.error_timeout)));
            } catch (IOException | JSONException e) {
                postError(callback, appContext.getString(R.string.error_sync_failed, e.getMessage()));
            } finally {
                db.close();
                UPLOAD_RUNNING.set(false);
            }
        });
    }

    private void postError(SyncCallback callback, String message) {
        MAIN_HANDLER.post(() -> callback.onError(message));
    }

    // ---------------------------------------------------------------------
    // Payload Generation
    // ---------------------------------------------------------------------

    /** Encapsulates JSON string payload and total item counts. */
    private static class Payload {
        String json;
        int eventCount;
        int sessionCount;
    }

    private Payload buildPayload(DatabaseHelper db, List<Event> events) throws JSONException {
        JSONObject root = new JSONObject();
        int totalSessions = 0;

        for (Event event : events) {
            JSONObject eventJson = new JSONObject();
            eventJson.put("name", event.getName());
            eventJson.put("date", event.getDate());
            eventJson.put("location", event.getLocation());
            eventJson.put("organiser", event.getOrganiser());
            eventJson.put("description", event.getDescription());
            eventJson.put("contactEmail", event.getContactEmail());
            eventJson.put("contactPhone", event.getContactPhone());

            List<Session> sessions = db.getSessions(event.getId(), null);
            if (!sessions.isEmpty()) {
                JSONObject sessionsJson = new JSONObject();
                for (Session session : sessions) {
                    JSONObject sessionJson = new JSONObject();
                    sessionJson.put("name", session.getName());
                    sessionJson.put("time", session.getTime());
                    sessionJson.put("speaker", session.getSpeaker());
                    sessionJson.put("room", session.getRoom());
                    sessionJson.put("type", session.getType());
                    sessionJson.put("description", session.getDescription());
                    // Prefix keys with 's' to avoid Firebase interpreting sequential numeric keys as JSON arrays
                    sessionsJson.put("s" + session.getId(), sessionJson);
                }
                eventJson.put("sessions", sessionsJson);
                totalSessions += sessions.size();
            }
            // Use cloud UUID as key for cross-device consistency
            root.put(event.getCloudId(), eventJson);
        }

        Payload payload = new Payload();
        payload.json = root.toString();
        payload.eventCount = events.size();
        payload.sessionCount = totalSessions;
        return payload;
    }

    // ---------------------------------------------------------------------
    // HTTP Operations
    // ---------------------------------------------------------------------

    /**
     * Executes HTTP PUT request sending JSON payload to Firebase endpoint.
     *
     * @param json serialized JSON payload string
     * @throws IOException on network failures or non-2xx HTTP response codes
     */
    private void putJson(String json) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(EVENTS_URL).openConnection();
        try {
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new IOException(appContext.getString(R.string.error_permission_denied));
            }
            if (code < 200 || code >= 300) {
                throw new IOException(appContext.getString(R.string.error_http, code));
            }
        } finally {
            connection.disconnect();
        }
    }
}
