package com.audit.exception;

/**
 * USER-DEFINED EXCEPTION — AuditException (base class)
 *
 * Root of the custom exception hierarchy for this system.
 * All application-specific exceptions extend this.
 *
 * Hierarchy:
 *   AuditException
 *     ├── AuthException          (login / auth failures)
 *     ├── UserNotFoundException  (user lookup failures)
 *     ├── AccessDeniedException  (role/permission violations)
 *     └── ValidationException    (bad input data)
 */
public class AuditException extends Exception {

    private final String errorCode;

    public AuditException(String message) {
        super(message);
        this.errorCode = "AUDIT_ERR";
    }

    public AuditException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuditException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
