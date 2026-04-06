package com.audit.exception;

/**
 * USER-DEFINED EXCEPTION — AccessDeniedException
 *
 * Thrown when a user attempts an operation they are not authorised for,
 * e.g. a USER-role trying to delete another account.
 *
 * INHERITANCE: extends AuditException
 */
public class AccessDeniedException extends AuditException {

    private final String requiredRole;
    private final String actualRole;

    public AccessDeniedException(String requiredRole, String actualRole) {
        super("ACCESS_DENIED",
              "Access denied. Required role: " + requiredRole +
              ", but current role is: " + actualRole);
        this.requiredRole = requiredRole;
        this.actualRole   = actualRole;
    }

    public String getRequiredRole() { return requiredRole; }
    public String getActualRole()   { return actualRole; }
}
