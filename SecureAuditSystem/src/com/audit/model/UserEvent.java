package com.audit.model;

import java.time.LocalDateTime;

public class UserEvent {
    private int    userEventId;
    private int    targetUserId;
    private int    performedBy;
    private String eventType;
    private LocalDateTime timestamp;

    public UserEvent() {}

    public UserEvent(int targetUserId, int performedBy, String eventType) {
        this.targetUserId = targetUserId;
        this.performedBy  = performedBy;
        this.eventType    = eventType;
    }

    public int    getUserEventId()                { return userEventId; }
    public void   setUserEventId(int id)          { this.userEventId = id; }
    public int    getTargetUserId()               { return targetUserId; }
    public void   setTargetUserId(int id)         { this.targetUserId = id; }
    public int    getPerformedBy()                { return performedBy; }
    public void   setPerformedBy(int id)          { this.performedBy = id; }
    public String getEventType()                  { return eventType; }
    public void   setEventType(String t)          { this.eventType = t; }
    public LocalDateTime getTimestamp()           { return timestamp; }
    public void   setTimestamp(LocalDateTime ts)  { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("%-5d | Target:%-4d | By:%-4d | %-12s | %s",
                userEventId, targetUserId, performedBy, eventType, timestamp);
    }
}
