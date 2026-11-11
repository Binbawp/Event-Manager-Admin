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
 * Uploads every event (with its sessions) to the Firebase Realtime Database (feature e).
 *
 * How it works:
 *  - The whole local database is turned into one JSON document and sent with a single
 *    HTTP PUT to /events.json, so ALL events are uploaded at once.
 *  - PUT replaces what is stored in the cloud, so the cloud always becomes an exact copy of
 *    the local database. Events or sessions deleted on the phone disappear from the cloud too.
 *  - The network work runs on a background thread (never the UI thread, which would freeze
 *    the app); results are posted back to the UI thread through a Handler.
 *
 * Cloud structure (the hybrid user app reads exactly this):
 *   events / {eventCloudId} / name, date, location, organiser, description,
 *                             contactEmail, contactPhone,
 *                             sessions / s{sessionId} / name, time, speaker, room, type, description
 */
public class CloudSyncManager {

    /** Base address of the Firebase Realtime Database (no trailing slash). */
    private static final String FIREBASE_URL =
            "https://event-manager-ac75b-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String EVENTS_URL = FIREBASE_URL + "/events.json";
    private static final int TIMEOUT_MS = 15000;

    /** One worker thread, so two uploads can never run at the same time. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean UPLOAD_RUNNING = new AtomicBoolean(false);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Results are always delivered on the UI thread. */
    public interface SyncCallback {
        void onSuccess(int eventCount, int sessionCount);

        /** @param message a complete, user-friendly message that can be shown as it is */
        void onError(String message);
    }

    private final Context appContext;

    public CloudSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Uploads everything now (if there is a connection). Must be called from the UI thread. */
    public void syncNow(SyncCallback callback) {
        if (!NetworkUtils.isOnline(appContext)) {
            callback.onError(appContext.getString(R.string.error_offline));
            return;
        }
        if (!UPLOAD_RUNNING.compareAndSet(false, true)) {
            callback.onError(appContext.getString(R.string.error_sync_busy));
            return;
        }

        // Remember which version of the data this upload is built from (see SyncPrefs)
        final long counterAtStart = SyncPrefs.getChangeCounter(appContext);

        EXECUTOR.execute(() -> {
            DatabaseHelper db = new DatabaseHelper(appContext);
            try {
                List<Event> events = db.getEvents(null);

                // Safety net: a brand-new install with no events must not wipe the cloud data
                if (events.isEmpty() && !SyncPrefs.hasEverSynced(appContext)) {
                    postError(callback, appContext.getString(R.string.error_nothing_to_upload));
                    return;
                }

                Payload payload = buildPayload(db, events);
                putJson(payload.json);

                // Only flag the events as uploaded if nothing changed while we were uploading
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
    // Building the JSON
    // ---------------------------------------------------------------------

    /** The JSON text to upload plus the totals shown to the admin afterwards. */
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
                    // The "s" prefix matters: Firebase turns an object whose keys are small
                    // consecutive numbers (1, 2, 3) into an ARRAY, which would break the user app.
                    sessionsJson.put("s" + session.getId(), sessionJson);
                }
                eventJson.put("sessions", sessionsJson);
                totalSessions += sessions.size();
            }
            // The UUID (not the local auto-number) is the key, so it is the same on every device
            root.put(event.getCloudId(), eventJson);
        }

        Payload payload = new Payload();
        payload.json = root.toString();
        payload.eventCount = events.size();
        payload.sessionCount = totalSessions;
        return payload;
    }

    // ---------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------

    /** Sends the JSON with an HTTP PUT. Throws IOException if the server does not accept it. */
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
