package com.example.eventassistant;

import java.io.Serializable;

/**
 * Data model representing a session record associated with an event.
 * Implements {@link Serializable} to allow passing Session objects between Android components via Intent extras.
 */
public class Session implements Serializable {

    private long id;            // Local auto-incremented session primary key
    private long eventId;       // Foreign key referencing the parent event ID
    private String name;        // Session title (required)
    private String time;        // Scheduled start time formatted as HH:mm (required)
    private String speaker;     // Speaker name (optional)
    private String room;        // Room location/number (optional)
    private String type;        // Session classification category (e.g., Keynote, Workshop, Presentation)
    private String description; // Optional detailed session description

    public Session() { }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEventId() { return eventId; }
    public void setEventId(long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
