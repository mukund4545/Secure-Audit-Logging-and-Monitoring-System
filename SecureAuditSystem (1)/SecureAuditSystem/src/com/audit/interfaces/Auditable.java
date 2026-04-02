package com.audit.interfaces;

/**
 * INTERFACE — Auditable
 *
 * Every entity that can be stored as an audit log record must implement this.
 * Enforces a contract: all log types must be able to describe themselves,
 * provide a category, and return their primary key.
 *
 * Implemented by: AuthLog, PasswordEvent, UserEvent, Session, InputValidationLog
 */
public interface Auditable {

    /**
     * Returns a one-line human-readable summary of this audit record.
     * Used for quick display without full toString().
     */
    String getSummary();

    /**
     * Returns the category/module this log belongs to.
     * e.g. "AUTHENTICATION", "PASSWORD", "SESSION", etc.
     */
    String getCategory();

    /**
     * Returns the primary key (log ID) of this record.
     */
    int getLogId();
}
