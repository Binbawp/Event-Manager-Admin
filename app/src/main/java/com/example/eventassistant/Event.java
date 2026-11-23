package com.example.eventassistant;

import java.io.Serializable;

/**
 * Data model representing an event record in the database.
 * Implements {@link Serializable} to allow passing Event objects between Android components via Intent extras.
 */
public class Event implements Serializable {

    private long id;                 // Local auto-incremented primary key
    private String cloudId;          // Unique UUID key used for cloud database synchronization
    private String name;             // Event title (required)
    private String date;             // Event date formatted as DD/MM/YYYY (required)
    private String location;         // Event location/venue
    private String organiser;        // Organizing entity or person
    private String description;      // Optional detailed event description
    private String contactEmail;     // Optional contact email address
    private String contactPhone;     // Optional contact phone number
    private boolean synced;          // Sync state: false if pending cloud upload, true if synchronized
    private int sessionCount;        // Dynamic session count calculated via query, not persisted in table

    public Event() { }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCloudId() { return cloudId; }
    public void setCloudId(String cloudId) { this.cloudId = cloudId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganiser() { return organiser; }
    public void setOrganiser(String organiser) { this.organiser = organiser; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
}
