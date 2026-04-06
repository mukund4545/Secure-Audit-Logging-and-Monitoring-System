package com.audit.exception;

/**
 * USER-DEFINED EXCEPTION — ValidationException
 *
 * Thrown when input data fails validation:
 *  - Empty username/email
 *  - Invalid email format
 *  - Password too short
 *  - Invalid enum value (role, status)
 *
 * INHERITANCE: extends AuditException
 */
public class ValidationException extends AuditException {

    private final String fieldName;
    private final String rejectedValue;

    public ValidationException(String fieldName, String rejectedValue, String reason) {
        super("VALIDATION_FAILED",
              "Validation failed for field '" + fieldName +
              "' with value '" + rejectedValue + "': " + reason);
        this.fieldName     = fieldName;
        this.rejectedValue = rejectedValue;
    }

    public String getFieldName()     { return fieldName; }
    public String getRejectedValue() { return rejectedValue; }
}
