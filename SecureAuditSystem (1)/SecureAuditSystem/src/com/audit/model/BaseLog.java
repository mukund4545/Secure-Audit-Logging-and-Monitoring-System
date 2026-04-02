package com.audit.model;

import com.audit.interfaces.Auditable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ABSTRACT CLASS — BaseLog
 *
 * INHERITANCE root for all audit log models:
 *   BaseLog
 *     ├── AuthLog
 *     ├── PasswordEvent
 *     ├── UserEvent
 *     ├── Session
 *     └── InputValidationLog
 *
 * Implements Auditable (INTERFACE).
 * Provides shared fields (userId, timestamp) and
 * declares abstract getSummary() — forcing POLYMORPHISM
 * in every subclass.
 */
public abstract class BaseLog implements Auditable {

    protected int           userId;
    protected LocalDateTime timestamp;

    protected static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---- Shared getters/setters ----
    public int getUserId()                         { return userId; }
    public void setUserId(int id)                  { this.userId = id; }
    public LocalDateTime getTimestamp()            { return timestamp; }
    public void setTimestamp(LocalDateTime ts)     { this.timestamp = ts; }

    /** Formatted timestamp string — shared utility for all subclasses */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FMT) : "N/A";
    }

    /**
     * POLYMORPHISM — abstract method.
     * Every subclass MUST override getSummary() to describe itself.
     * Implements Auditable.getSummary().
     */
    @Override
    public abstract String getSummary();

    /**
     * POLYMORPHISM — abstract method.
     * Every subclass MUST declare which module/category it belongs to.
     * Implements Auditable.getCategory().
     */
    @Override
    public abstract String getCategory();
}
