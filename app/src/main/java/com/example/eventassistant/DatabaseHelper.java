package com.example.eventassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite database helper managing local relational storage for events and sessions.
 * Maintains a one-to-many relationship between events and sessions linked via foreign keys
 * with {@code ON DELETE CASCADE} to clean up child sessions automatically when an event is deleted.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "event_manager.db";
    private static final int DATABASE_VERSION = 1;

    // ----- Events Table Schema -----
    public static final String TABLE_EVENTS = "events";
    public static final String COL_EVENT_ID = "event_id";
    public static final String COL_EVENT_CLOUD_ID = "cloud_id";
    public static final String COL_EVENT_NAME = "name";
    public static final String COL_EVENT_DATE = "date";
    public static final String COL_EVENT_LOCATION = "location";
    public static final String COL_EVENT_ORGANISER = "organiser";
    public static final String COL_EVENT_DESCRIPTION = "description";
    public static final String COL_EVENT_EMAIL = "contact_email";
    public static final String COL_EVENT_PHONE = "contact_phone";
    public static final String COL_EVENT_SYNCED = "synced";

    // ----- Sessions Table Schema -----
    public static final String TABLE_SESSIONS = "sessions";
    public static final String COL_SESSION_ID = "session_id";
    public static final String COL_SESSION_EVENT_ID = "event_id";
    public static final String COL_SESSION_NAME = "name";
    public static final String COL_SESSION_TIME = "time";
    public static final String COL_SESSION_SPEAKER = "speaker";
    public static final String COL_SESSION_ROOM = "room";
    public static final String COL_SESSION_TYPE = "type";
    public static final String COL_SESSION_DESCRIPTION = "description";

    // SQL statement to create the events table with unique constraint on name and date
    private static final String CREATE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " ("
                    + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_EVENT_CLOUD_ID + " TEXT NOT NULL UNIQUE, "
                    + COL_EVENT_NAME + " TEXT NOT NULL, "
                    + COL_EVENT_DATE + " TEXT NOT NULL, "
                    + COL_EVENT_LOCATION + " TEXT, "
                    + COL_EVENT_ORGANISER + " TEXT, "
                    + COL_EVENT_DESCRIPTION + " TEXT, "
                    + COL_EVENT_EMAIL + " TEXT, "
                    + COL_EVENT_PHONE + " TEXT, "
                    + COL_EVENT_SYNCED + " INTEGER NOT NULL DEFAULT 0, "
                    // Constraint: Duplicate events sharing both identical name and date are forbidden
                    + "UNIQUE(" + COL_EVENT_NAME + ", " + COL_EVENT_DATE + "))";

    // SQL statement to create the sessions table with foreign key constraint referencing events
    private static final String CREATE_SESSIONS =
            "CREATE TABLE " + TABLE_SESSIONS + " ("
                    + COL_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_SESSION_EVENT_ID + " INTEGER NOT NULL, "
                    + COL_SESSION_NAME + " TEXT NOT NULL, "
                    + COL_SESSION_TIME + " TEXT NOT NULL, "
                    + COL_SESSION_SPEAKER + " TEXT, "
                    + COL_SESSION_ROOM + " TEXT, "
                    + COL_SESSION_TYPE + " TEXT, "
                    + COL_SESSION_DESCRIPTION + " TEXT, "
                    + "FOREIGN KEY(" + COL_SESSION_EVENT_ID + ") REFERENCES "
                    + TABLE_EVENTS + "(" + COL_EVENT_ID + ") ON DELETE CASCADE)";

    // SQL query selecting event attributes along with subquery calculation for session count
    private static final String EVENT_SELECT =
            "SELECT e.*, (SELECT COUNT(*) FROM " + TABLE_SESSIONS + " s WHERE s."
                    + COL_SESSION_EVENT_ID + " = e." + COL_EVENT_ID + ") AS session_count FROM "
                    + TABLE_EVENTS + " e";

    // Chronological order by rebuilt date (YYYYMMDD) followed by event name
    private static final String EVENT_ORDER =
            " ORDER BY substr(e." + COL_EVENT_DATE + ",7,4) || substr(e." + COL_EVENT_DATE
                    + ",4,2) || substr(e." + COL_EVENT_DATE + ",1,2), e." + COL_EVENT_NAME;

    private final Context appContext;   // Application context used for recording change notifications

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    /**
     * Enables foreign key constraint enforcement for SQLite database connections.
     *
     * @param db the SQLite database instance
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_EVENTS);
        db.execSQL(CREATE_SESSIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop child table first to maintain foreign key integrity
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    // =====================================================================
    // Event Operations
    // =====================================================================

    /**
     * Inserts a new event record into the database with a generated cloud UUID.
     *
     * @param event the event object containing field values
     * @return the new row ID, or -1 if the insertion failed (e.g., due to duplicate name and date)
     */
    public long insertEvent(Event event) {
        ContentValues values = eventValues(event);
        values.put(COL_EVENT_CLOUD_ID, UUID.randomUUID().toString());
        long id = getWritableDatabase().insert(TABLE_EVENTS, null, values);
        if (id != -1) {
            recordChange();
        }
        return id;
    }

    /**
     * Updates an existing event record and marks it as unsynced.
     *
     * @param event the event object containing updated values
     * @return number of rows updated, or -1 if constraint validation fails
     */
    public int updateEvent(Event event) {
        ContentValues values = eventValues(event);
        try {
            int rows = getWritableDatabase().update(TABLE_EVENTS, values,
                    COL_EVENT_ID + " = ?", new String[]{String.valueOf(event.getId())});
            if (rows > 0) {
                recordChange();
            }
            return rows;
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    /**
     * Maps Event properties to {@link ContentValues} for database write operations.
     */
    private ContentValues eventValues(Event event) {
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_NAME, event.getName());
        values.put(COL_EVENT_DATE, event.getDate());
        values.put(COL_EVENT_LOCATION, event.getLocation());
        values.put(COL_EVENT_ORGANISER, event.getOrganiser());
        values.put(COL_EVENT_DESCRIPTION, event.getDescription());
        values.put(COL_EVENT_EMAIL, event.getContactEmail());
        values.put(COL_EVENT_PHONE, event.getContactPhone());
        values.put(COL_EVENT_SYNCED, 0);
        return values;
    }

    /**
     * Retrieves a single event by its database primary key.
     *
     * @param eventId the unique event database ID
     * @return the Event object with calculated session count, or null if not found
     */
    public Event getEvent(long eventId) {
        List<Event> result = queryEvents(" WHERE e." + COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)});
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Queries events with optional search filtering on name and description.
     *
     * @param query search string; null or empty retrieves all events
     * @return list of matching Event objects ordered chronologically
     */
    public List<Event> getEvents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return queryEvents("", null);
        }
        String q = query.trim();
        return queryEvents(" WHERE e." + COL_EVENT_NAME + " LIKE ? OR e." + COL_EVENT_DESCRIPTION
                + " LIKE ?", new String[]{q + "%", "%" + q + "%"});   // Parametrized query protects against SQL injection
    }

    /**
     * Searches events using multiple criteria (AND logic).
     *
     * @param date      exact event date (DD/MM/YYYY), or empty to ignore
     * @param organiser partial or full organiser name, or empty to ignore
     * @return list of matching Event objects
     */
    public List<Event> searchEvents(String date, String organiser) {
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();

        if (date != null && !date.trim().isEmpty()) {
            conditions.add("e." + COL_EVENT_DATE + " = ?");
            args.add(date.trim());
        }
        if (organiser != null && !organiser.trim().isEmpty()) {
            conditions.add("e." + COL_EVENT_ORGANISER + " LIKE ?");
            args.add("%" + organiser.trim() + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + TextUtils.join(" AND ", conditions);
        return queryEvents(where, args.toArray(new String[0]));
    }

    /**
     * Checks if another event record already exists with the given name and date combination.
     *
     * @param name           event name
     * @param date           event date
     * @param excludeEventId event ID to exclude (used when updating existing records)
     * @return true if a duplicate exists; false otherwise
     */
    public boolean eventExists(String name, String date, long excludeEventId) {
        String sql = "SELECT 1 FROM " + TABLE_EVENTS + " WHERE " + COL_EVENT_NAME
                + " = ? COLLATE NOCASE AND " + COL_EVENT_DATE + " = ? AND " + COL_EVENT_ID
                + " != ? LIMIT 1";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[]{name, date, String.valueOf(excludeEventId)})) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Deletes an event record from the database. Child sessions are deleted via ON DELETE CASCADE.
     *
     * @param eventId primary key of the event to delete
     * @return number of rows deleted
     */
    public int deleteEvent(long eventId) {
        int rows = getWritableDatabase().delete(TABLE_EVENTS,
                COL_EVENT_ID + " = ?", new String[]{String.valueOf(eventId)});
        if (rows > 0) {
            recordChange();
        }
        return rows;
    }

    private List<Event> queryEvents(String where, String[] args) {
        List<Event> events = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(EVENT_SELECT + where + EVENT_ORDER, args)) {
            while (cursor.moveToNext()) {
                events.add(eventFromCursor(cursor));
            }
        }
        return events;
    }

    private Event eventFromCursor(Cursor c) {
        Event event = new Event();
        event.setId(c.getLong(c.getColumnIndexOrThrow(COL_EVENT_ID)));
        event.setCloudId(c.getString(c.getColumnIndexOrThrow(COL_EVENT_CLOUD_ID)));
        event.setName(c.getString(c.getColumnIndexOrThrow(COL_EVENT_NAME)));
        event.setDate(c.getString(c.getColumnIndexOrThrow(COL_EVENT_DATE)));
        event.setLocation(c.getString(c.getColumnIndexOrThrow(COL_EVENT_LOCATION)));
        event.setOrganiser(c.getString(c.getColumnIndexOrThrow(COL_EVENT_ORGANISER)));
        event.setDescription(c.getString(c.getColumnIndexOrThrow(COL_EVENT_DESCRIPTION)));
        event.setContactEmail(c.getString(c.getColumnIndexOrThrow(COL_EVENT_EMAIL)));
        event.setContactPhone(c.getString(c.getColumnIndexOrThrow(COL_EVENT_PHONE)));
        event.setSynced(c.getInt(c.getColumnIndexOrThrow(COL_EVENT_SYNCED)) == 1);
        event.setSessionCount(c.getInt(c.getColumnIndexOrThrow("session_count")));
        return event;
    }

    // =====================================================================
    // Session Operations
    // =====================================================================

    /**
     * Inserts a session record associated with a parent event.
     *
     * @param session the Session object to insert
     * @return the generated session row ID, or -1 on failure
     */
    public long insertSession(Session session) {
        long id = getWritableDatabase().insert(TABLE_SESSIONS, null, sessionValues(session));
        if (id != -1) {
            markEventUnsynced(session.getEventId());
        }
        return id;
    }

    /**
     * Updates an existing session record.
     *
     * @param session the Session object containing updated values
     * @return number of rows updated
     */
    public int updateSession(Session session) {
        int rows = getWritableDatabase().update(TABLE_SESSIONS, sessionValues(session),
                COL_SESSION_ID + " = ?", new String[]{String.valueOf(session.getId())});
        if (rows > 0) {
            markEventUnsynced(session.getEventId());
        }
        return rows;
    }

    /**
     * Deletes a session record.
     *
     * @param session the Session object to delete
     * @return number of rows deleted
     */
    public int deleteSession(Session session) {
        int rows = getWritableDatabase().delete(TABLE_SESSIONS,
                COL_SESSION_ID + " = ?", new String[]{String.valueOf(session.getId())});
        if (rows > 0) {
            markEventUnsynced(session.getEventId());
        }
        return rows;
    }

    /**
     * Retrieves a session record by its database ID.
     *
     * @param sessionId the session ID
     * @return the Session object, or null if not found
     */
    public Session getSession(long sessionId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_SESSIONS + " WHERE " + COL_SESSION_ID + " = ?",
                new String[]{String.valueOf(sessionId)})) {
            return cursor.moveToFirst() ? sessionFromCursor(cursor) : null;
        }
    }

    /**
     * Retrieves sessions for a specific event with optional search filtering, ordered by time.
     *
     * @param eventId parent event ID
     * @param query   search string; null or empty retrieves all sessions for the event
     * @return list of matching Session objects
     */
    public List<Session> getSessions(long eventId, String query) {
        List<Session> sessions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(eventId));

        String sql = "SELECT * FROM " + TABLE_SESSIONS + " WHERE " + COL_SESSION_EVENT_ID + " = ?";
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim();
            sql += " AND (" + COL_SESSION_NAME + " LIKE ? OR " + COL_SESSION_DESCRIPTION + " LIKE ?)";
            args.add(q + "%");
            args.add("%" + q + "%");
        }
        sql += " ORDER BY " + COL_SESSION_TIME + ", " + COL_SESSION_NAME;

        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                sessions.add(sessionFromCursor(cursor));
            }
        }
        return sessions;
    }

    private ContentValues sessionValues(Session session) {
        ContentValues values = new ContentValues();
        values.put(COL_SESSION_EVENT_ID, session.getEventId());
        values.put(COL_SESSION_NAME, session.getName());
        values.put(COL_SESSION_TIME, session.getTime());
        values.put(COL_SESSION_SPEAKER, session.getSpeaker());
        values.put(COL_SESSION_ROOM, session.getRoom());
        values.put(COL_SESSION_TYPE, session.getType());
        values.put(COL_SESSION_DESCRIPTION, session.getDescription());
        return values;
    }

    private Session sessionFromCursor(Cursor c) {
        Session session = new Session();
        session.setId(c.getLong(c.getColumnIndexOrThrow(COL_SESSION_ID)));
        session.setEventId(c.getLong(c.getColumnIndexOrThrow(COL_SESSION_EVENT_ID)));
        session.setName(c.getString(c.getColumnIndexOrThrow(COL_SESSION_NAME)));
        session.setTime(c.getString(c.getColumnIndexOrThrow(COL_SESSION_TIME)));
        session.setSpeaker(c.getString(c.getColumnIndexOrThrow(COL_SESSION_SPEAKER)));
        session.setRoom(c.getString(c.getColumnIndexOrThrow(COL_SESSION_ROOM)));
        session.setType(c.getString(c.getColumnIndexOrThrow(COL_SESSION_TYPE)));
        session.setDescription(c.getString(c.getColumnIndexOrThrow(COL_SESSION_DESCRIPTION)));
        return session;
    }

    /** Marks parent event as unsynced following session modifications. */
    private void markEventUnsynced(long eventId) {
        getWritableDatabase().execSQL("UPDATE " + TABLE_EVENTS + " SET " + COL_EVENT_SYNCED
                + " = 0 WHERE " + COL_EVENT_ID + " = ?", new Object[]{eventId});
        recordChange();
    }

    /** Marks all events as synchronized following a successful cloud upload. */
    public void markAllEventsSynced() {
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_SYNCED, 1);
        getWritableDatabase().update(TABLE_EVENTS, values, null, null);
    }

    /** Records a change notification in shared preferences. */
    private void recordChange() {
        SyncPrefs.recordChange(appContext);
    }

    // =====================================================================
    // Sample Test Data Initialization
    // =====================================================================

    /** Inserts sample event and session records into the database for demonstration testing. */
    public void insertSampleData() {
        long id1 = insertSampleEvent("Tech Conference 2026", "12/11/2026", "London",
                "Greenwich Tech Society", "Annual conference on mobile and cloud technology");
        insertSampleEvent("Career Fair", "20/11/2026", "Manchester",
                "Greenwich Careers", "Meet employers hiring graduates");
        insertSampleEvent("Hack Night", "05/12/2026", "Online",
                "Code Club", "Overnight hackathon for beginners");

        if (id1 != -1) {
            insertSampleSession(id1, "Opening Keynote", "09:00", "Keynote");
            insertSampleSession(id1, "Android Workshop", "11:00", "Workshop");
            insertSampleSession(id1, "Networking Lunch", "13:00", "Networking");
        }
    }

    private long insertSampleEvent(String name, String date, String location,
                                   String organiser, String description) {
        Event e = new Event();
        e.setName(name);
        e.setDate(date);
        e.setLocation(location);
        e.setOrganiser(organiser);
        e.setDescription(description);
        return insertEvent(e);
    }

    private void insertSampleSession(long eventId, String name, String time, String type) {
        Session s = new Session();
        s.setEventId(eventId);
        s.setName(name);
        s.setTime(time);
        s.setType(type);
        insertSession(s);
    }
}
