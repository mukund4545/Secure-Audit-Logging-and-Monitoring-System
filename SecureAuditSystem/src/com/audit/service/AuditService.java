package com.audit.service;

import com.audit.dao.*;
import com.audit.model.*;
import com.audit.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer - contains all business logic.
 * Follows OOP: encapsulation via DAOs, single-responsibility per method.
 */
public class AuditService {

    private final UserDAO              userDAO              = new UserDAO();
    private final AuthLogDAO           authLogDAO           = new AuthLogDAO();
    private final PasswordEventDAO     passwordEventDAO     = new PasswordEventDAO();
    private final UserEventDAO         userEventDAO         = new UserEventDAO();
    private final SessionDAO           sessionDAO           = new SessionDAO();
    private final InputValidationLogDAO validationLogDAO    = new InputValidationLogDAO();

    // ================================================================
    //  USER MANAGEMENT
    // ================================================================

    public String registerUser(String username, String email, String plainPassword,
                               String role, int performedBy) {
        try {
            if (username == null || username.trim().isEmpty())
                return "ERROR: Username cannot be empty.";
            if (!email.contains("@"))
                return "ERROR: Invalid email format.";
            if (plainPassword.length() < 6)
                return "ERROR: Password must be at least 6 characters.";

            String hash = PasswordUtil.hash(plainPassword);
            User newUser = new User(username.trim(), email.trim(), hash, role, "ACTIVE");
            boolean ok = userDAO.createUser(newUser);
            if (ok) {
                // log user event
                UserEvent ue = new UserEvent(newUser.getUserId(), performedBy, "CREATED");
                userEventDAO.insert(ue);
                return "SUCCESS: User '" + username + "' created with ID " + newUser.getUserId();
            }
            return "ERROR: Failed to create user.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public List<User> listAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    public String changeUserRole(int targetId, String newRole, int performedBy) {
        try {
            boolean ok = userDAO.updateRole(targetId, newRole);
            if (ok) {
                userEventDAO.insert(new UserEvent(targetId, performedBy, "ROLE_CHANGED"));
                return "SUCCESS: Role updated to " + newRole;
            }
            return "ERROR: User not found.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public String changeUserStatus(int targetId, String newStatus, int performedBy) {
        try {
            boolean ok = userDAO.updateStatus(targetId, newStatus);
            if (ok) {
                String evType = newStatus.equals("LOCKED") ? "LOCKED" :
                                newStatus.equals("ACTIVE")  ? "UNLOCKED" : "MODIFIED";
                userEventDAO.insert(new UserEvent(targetId, performedBy, evType));
                return "SUCCESS: Status updated to " + newStatus;
            }
            return "ERROR: User not found.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public String deleteUser(int targetId, int performedBy) {
        try {
            // log before delete (FK cascade will remove child records)
            userEventDAO.insert(new UserEvent(targetId, performedBy, "DELETED"));
            boolean ok = userDAO.deleteUser(targetId);
            return ok ? "SUCCESS: User deleted." : "ERROR: User not found.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    // ================================================================
    //  AUTHENTICATION
    // ================================================================

    /**
     * Attempts login; returns logged-in User on success, null on failure.
     */
    public User login(String username, String plainPassword, String sourceIp) {
        try {
            User user = userDAO.getUserByUsername(username);

            if (user == null) {
                authLogDAO.insert(new AuthLog(0, "LOGIN", "FAILURE", sourceIp));
                return null;
            }
            if (!user.getAccountStatus().equals("ACTIVE")) {
                authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "BLOCKED", sourceIp));
                return null;
            }
            if (!PasswordUtil.verify(plainPassword, user.getPasswordHash())) {
                authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "FAILURE", sourceIp));
                return null;
            }

            authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "SUCCESS", sourceIp));
            return user;
        } catch (SQLException e) {
            System.err.println("DB ERROR during login: " + e.getMessage());
            return null;
        }
    }

    public void recordLogout(int userId, String sourceIp) {
        try {
            authLogDAO.insert(new AuthLog(userId, "LOGOUT", "SUCCESS", sourceIp));
        } catch (SQLException e) {
            System.err.println("DB ERROR recording logout: " + e.getMessage());
        }
    }

    public List<AuthLog> getAuthLogs() throws SQLException {
        return authLogDAO.getAllLogs();
    }

    public List<AuthLog> getFailedLogins() throws SQLException {
        return authLogDAO.getFailedLogins();
    }

    public List<AuthLog> getAuthLogsByUser(int userId) throws SQLException {
        return authLogDAO.getLogsByUser(userId);
    }

    // ================================================================
    //  PASSWORD EVENTS
    // ================================================================

    public String changePassword(int userId, String oldPlain, String newPlain, String sourceIp) {
        try {
            User user = userDAO.getUserById(userId);
            if (user == null) return "ERROR: User not found.";

            if (!PasswordUtil.verify(oldPlain, user.getPasswordHash())) {
                passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "FAILURE"));
                validationLogDAO.insert(new InputValidationLog(userId, "password", "Old password mismatch", sourceIp));
                return "ERROR: Old password is incorrect.";
            }
            if (newPlain.length() < 6) {
                passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "FAILURE"));
                return "ERROR: New password too short.";
            }

            userDAO.updatePassword(userId, PasswordUtil.hash(newPlain));
            passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "SUCCESS"));
            return "SUCCESS: Password changed successfully.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public String requestPasswordReset(int userId) {
        try {
            User user = userDAO.getUserById(userId);
            if (user == null) return "ERROR: User not found.";
            passwordEventDAO.insert(new PasswordEvent(userId, "RESET_REQUEST", "SUCCESS"));
            return "SUCCESS: Password reset request logged for user " + userId;
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public List<PasswordEvent> getPasswordEvents() throws SQLException {
        return passwordEventDAO.getAll();
    }

    // ================================================================
    //  SESSION MANAGEMENT
    // ================================================================

    public int openSession(int userId, String sourceIp) {
        try {
            Session s = new Session(userId, sourceIp);
            sessionDAO.createSession(s);
            return s.getSessionId();
        } catch (SQLException e) {
            System.err.println("DB ERROR opening session: " + e.getMessage());
            return -1;
        }
    }

    public String terminateSession(int sessionId) {
        try {
            boolean ok = sessionDAO.terminateSession(sessionId, "TERMINATED");
            return ok ? "SUCCESS: Session " + sessionId + " terminated." : "ERROR: Session not found.";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public List<Session> getAllSessions() throws SQLException {
        return sessionDAO.getAll();
    }

    public List<Session> getActiveSessions() throws SQLException {
        return sessionDAO.getActiveSessions();
    }

    // ================================================================
    //  INPUT VALIDATION LOGS
    // ================================================================

    public String logValidationFailure(int userId, String field, String reason, String sourceIp) {
        try {
            InputValidationLog log = new InputValidationLog(userId, field, reason, sourceIp);
            validationLogDAO.insert(log);
            return "SUCCESS: Validation failure logged (ID=" + log.getValidationLogId() + ")";
        } catch (SQLException e) {
            return "DB ERROR: " + e.getMessage();
        }
    }

    public List<InputValidationLog> getValidationLogs() throws SQLException {
        return validationLogDAO.getAll();
    }

    // ================================================================
    //  USER EVENTS
    // ================================================================

    public List<UserEvent> getUserEvents() throws SQLException {
        return userEventDAO.getAll();
    }
}
