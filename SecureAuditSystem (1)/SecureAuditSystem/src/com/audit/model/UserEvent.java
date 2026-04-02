package com.audit.model;

/**
 * INHERITANCE: extends BaseLog
 * POLYMORPHISM: overrides getSummary(), getCategory(), getLogId()
 */
public class UserEvent extends BaseLog {

    private int    userEventId;
    private int    targetUserId;
    private int    performedBy;
    private String eventType;

    public UserEvent() {}

    public UserEvent(int targetUserId, int performedBy, String eventType) {
        this.targetUserId = targetUserId;
        this.performedBy  = performedBy;
        this.eventType    = eventType;
        // userId in BaseLog = the one who performed the action
        this.userId       = performedBy;
    }

    public int    getUserEventId()          { return userEventId; }
    public void   setUserEventId(int id)    { this.userEventId = id; }
    public int    getTargetUserId()         { return targetUserId; }
    public void   setTargetUserId(int id)   { this.targetUserId = id; }
    public int    getPerformedBy()          { return performedBy; }
    public void   setPerformedBy(int id)    { this.performedBy = id; userId = id; }
    public String getEventType()            { return eventType; }
    public void   setEventType(String t)    { this.eventType = t; }

    @Override public int    getLogId()    { return userEventId; }
    @Override public String getCategory(){ return "USER_EVENT"; }

    @Override
    public String getSummary() {
        return String.format("[USER_EVENT] Target:%d | By:%d | %s | %s",
                targetUserId, performedBy, eventType, getFormattedTimestamp());
    }

    @Override
    public String toString() {
        return String.format("%-5d | Target:%-4d | By:%-4d | %-12s | %s",
                userEventId, targetUserId, performedBy, eventType, getFormattedTimestamp());
    }
}
