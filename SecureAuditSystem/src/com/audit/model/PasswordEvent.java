package com.audit.model;

import java.time.LocalDateTime;

public class PasswordEvent {
    private int    passwordEventId;
    private int    userId;
    private String eventType;
    private String eventStatus;
    private LocalDateTime timestamp;

    public PasswordEvent() {}

    public PasswordEvent(int userId, String eventType, String eventStatus) {
        this.userId      = userId;
        this.eventType   = eventType;
        this.eventStatus = eventStatus;
    }

    public int    getPasswordEventId()                 { return passwordEventId; }
    public void   setPasswordEventId(int id)           { this.passwordEventId = id; }
    public int    getUserId()                          { return userId; }
    public void   setUserId(int id)                    { this.userId = id; }
    public String getEventType()                       { return eventType; }
    public void   setEventType(String t)               { this.eventType = t; }
    public String getEventStatus()                     { return eventStatus; }
    public void   setEventStatus(String s)             { this.eventStatus = s; }
    public LocalDateTime getTimestamp()                { return timestamp; }
    public void   setTimestamp(LocalDateTime ts)       { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-16s | %-8s | %s",
                passwordEventId, userId, eventType, eventStatus, timestamp);
    }
}
