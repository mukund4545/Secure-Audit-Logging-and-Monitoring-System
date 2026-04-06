package com.audit.exception;

/**
 * USER-DEFINED EXCEPTION — UserNotFoundException
 *
 * Thrown when a user lookup by ID or username returns no result.
 *
 * INHERITANCE: extends AuditException
 */
public class UserNotFoundException extends AuditException {

    private final int    userId;
    private final String username;

    /** Thrown when lookup was by ID */
    public UserNotFoundException(int userId) {
        super("USER_NOT_FOUND", "No user found with ID: " + userId);
        this.userId   = userId;
        this.username = null;
    }

    /** Thrown when lookup was by username */
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", "No user found with username: '" + username + "'");
        this.userId   = -1;
        this.username = username;
    }

    public int    getUserId()   { return userId; }
    public String getUsername() { return username; }
}
