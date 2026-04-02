package com.audit.exception;

/**
 * USER-DEFINED EXCEPTION — AuthException
 *
 * Thrown when authentication fails:
 *  - Wrong password
 *  - Account locked / suspended / inactive
 *  - Username not found during login
 *
 * INHERITANCE: extends AuditException
 */
public class AuthException extends AuditException {

    public enum Reason {
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        ACCOUNT_INACTIVE,
        ACCOUNT_SUSPENDED,
        USER_NOT_FOUND
    }

    private final Reason reason;

    public AuthException(Reason reason, String message) {
        super("AUTH_" + reason.name(), message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
