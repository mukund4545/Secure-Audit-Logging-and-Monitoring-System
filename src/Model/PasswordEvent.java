package com.audit.model;

/**
 * INHERITANCE: extends BaseLog
 * POLYMORPHISM: overrides getSummary(), getCategory(), getLogId()
 */
public class PasswordEvent extends BaseLog {

    private int    passwordEventId;
    private String eventType;
    private String eventStatus;

    public PasswordEvent() {}

    public PasswordEvent(int userId, String eventType, String eventStatus) {
        this.userId      = userId;
        this.eventType   = eventType;
        this.eventStatus = eventStatus;
    }

    public int    getPasswordEventId()       { return passwordEventId; }
    public void   setPasswordEventId(int id) { this.passwordEventId = id; }
    public String getEventType()             { return eventType; }
    public void   setEventType(String t)     { this.eventType = t; }
    public String getEventStatus()           { return eventStatus; }
    public void   setEventStatus(String s)   { this.eventStatus = s; }

    @Override public int    getLogId()    { return passwordEventId; }
    @Override public String getCategory(){ return "PASSWORD"; }

    @Override
    public String getSummary() {
        return String.format("[PASSWORD] User:%d | %s | %s | %s",
                userId, eventType, eventStatus, getFormattedTimestamp());
    }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-16s | %-8s | %s",
                passwordEventId, userId, eventType, eventStatus, getFormattedTimestamp());
    }
}
