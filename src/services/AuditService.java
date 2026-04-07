package com.audit.service;

import com.audit.dao.*;
import com.audit.exception.*;
import com.audit.interfaces.BaseDAO;
import com.audit.model.*;
import com.audit.util.PasswordUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer — all business logic lives here.
 *
 * OOP concepts demonstrated:
 *  - POLYMORPHISM : printAllLogs() iterates List<BaseDAO<?>> and calls
 *                   getSummary() on every BaseLog subtype uniformly.
 *  - INTERFACE    : DAOs are referenced via BaseDAO<T>.
 *  - INHERITANCE  : log objects returned are BaseLog subtypes.
 *  - EXCEPTIONS   : every error path throws a typed AuditException subclass.
 */
public class AuditService {

    // ---- DAOs (referenced via their concrete types for extra methods) ----
    private final UserDAO               userDAO           = new UserDAO();
    private final AuthLogDAO            authLogDAO        = new AuthLogDAO();
    private final PasswordEventDAO      passwordEventDAO  = new PasswordEventDAO();
    private final UserEventDAO          userEventDAO      = new UserEventDAO();
    private final SessionDAO            sessionDAO        = new SessionDAO();
    private final InputValidationLogDAO validationLogDAO  = new InputValidationLogDAO();
    private final ProcedureDAO          procedureDAO      = new ProcedureDAO();

    /**
     * POLYMORPHISM SHOWCASE — all DAO types stored as BaseDAO<?>.
     * Calling getAll() on each returns List<? extends BaseLog>.
     * getSummary() dispatches to the correct subclass at runtime.
     */
    public List<String> getAllLogSummaries() throws SQLException {
        List<BaseDAO<?>> allDAOs = new ArrayList<>();
        allDAOs.add(authLogDAO);
        allDAOs.add(passwordEventDAO);
        allDAOs.add(userEventDAO);
        allDAOs.add(sessionDAO);
        allDAOs.add(validationLogDAO);

        List<String> summaries = new ArrayList<>();
        for (BaseDAO<?> dao : allDAOs) {
            for (Object obj : dao.getAll()) {
                if (obj instanceof BaseLog) {
                    // POLYMORPHIC CALL — getSummary() dispatched per subtype
                    summaries.add(((BaseLog) obj).getSummary());
                }
            }
        }
        return summaries;
    }

    // ================================================================
    //  USER MANAGEMENT  — throws UserDefinedExceptions
    // ================================================================

    public User registerUser(String username, String email, String plainPassword,
                             String role, int performedBy)
            throws AuditException, SQLException {

        // --- ValidationException ---
        if (username == null || username.trim().isEmpty())
            throw new ValidationException("username", "", "Username cannot be empty");
        if (!email.contains("@") || !email.contains("."))
            throw new ValidationException("email", email, "Invalid email format");
        if (plainPassword.length() < 6)
            throw new ValidationException("password", "***", "Password must be at least 6 characters");
        if (!role.matches("ADMIN|ANALYST|USER"))
            throw new ValidationException("role", role, "Must be ADMIN, ANALYST, or USER");

        String hash    = PasswordUtil.hash(plainPassword);
        User   newUser = new User(username.trim(), email.trim(), hash, role, "ACTIVE");
        boolean ok     = userDAO.createUser(newUser);
        if (!ok) throw new AuditException("CREATE_FAILED", "Failed to create user in database");

        userEventDAO.insert(new UserEvent(newUser.getUserId(), performedBy, "CREATED"));
        return newUser;
    }

    public List<User> listAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    public User getUserById(int userId) throws AuditException, SQLException {
        User u = userDAO.getUserById(userId);
        if (u == null) throw new UserNotFoundException(userId);   // USER-DEFINED EXCEPTION
        return u;
    }

    public void changeUserRole(int targetId, String newRole, int performedBy)
            throws AuditException, SQLException {

        if (!newRole.matches("ADMIN|ANALYST|USER"))
            throw new ValidationException("role", newRole, "Must be ADMIN, ANALYST, or USER");

        User target = userDAO.getUserById(targetId);
        if (target == null) throw new UserNotFoundException(targetId);

        userDAO.updateRole(targetId, newRole);
        userEventDAO.insert(new UserEvent(targetId, performedBy, "ROLE_CHANGED"));
    }

    public void changeUserStatus(int targetId, String newStatus, int performedBy)
            throws AuditException, SQLException {

        if (!newStatus.matches("ACTIVE|INACTIVE|LOCKED|SUSPENDED"))
            throw new ValidationException("account_status", newStatus,
                    "Must be ACTIVE, INACTIVE, LOCKED, or SUSPENDED");

        User target = userDAO.getUserById(targetId);
        if (target == null) throw new UserNotFoundException(targetId);

        userDAO.updateStatus(targetId, newStatus);
        String evType = newStatus.equals("LOCKED") ? "LOCKED" :
                        newStatus.equals("ACTIVE")  ? "UNLOCKED" : "MODIFIED";
        userEventDAO.insert(new UserEvent(targetId, performedBy, evType));
    }

    public void deleteUser(int targetId, int performedBy)
            throws AuditException, SQLException {

        User target = userDAO.getUserById(targetId);
        if (target == null) throw new UserNotFoundException(targetId);

        userEventDAO.insert(new UserEvent(targetId, performedBy, "DELETED"));
        userDAO.deleteUser(targetId);
    }

    // ================================================================
    //  AUTHENTICATION  — throws AuthException
    // ================================================================

    /**
     * Login: throws typed AuthException on every failure path.
     * The calling menu catches it, prints the reason, and logs the attempt.
     */
    public User login(String username, String plainPassword, String sourceIp)
            throws AuthException, SQLException {

        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            authLogDAO.insert(new AuthLog(0, "LOGIN", "FAILURE", sourceIp));
            throw new AuthException(AuthException.Reason.USER_NOT_FOUND,
                    "No account found for username: '" + username + "'");
        }

        switch (user.getAccountStatus()) {
            case "LOCKED":
                authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "BLOCKED", sourceIp));
                throw new AuthException(AuthException.Reason.ACCOUNT_LOCKED,
                        "Account '" + username + "' is locked.");
            case "INACTIVE":
                authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "BLOCKED", sourceIp));
                throw new AuthException(AuthException.Reason.ACCOUNT_INACTIVE,
                        "Account '" + username + "' is inactive.");
            case "SUSPENDED":
                authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "BLOCKED", sourceIp));
                throw new AuthException(AuthException.Reason.ACCOUNT_SUSPENDED,
                        "Account '" + username + "' is suspended.");
        }

        if (!PasswordUtil.verify(plainPassword, user.getPasswordHash())) {
            authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "FAILURE", sourceIp));
            throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS,
                    "Incorrect password for user '" + username + "'");
        }

        authLogDAO.insert(new AuthLog(user.getUserId(), "LOGIN", "SUCCESS", sourceIp));
        return user;
    }

    public void recordLogout(int userId, String sourceIp) throws SQLException {
        authLogDAO.insert(new AuthLog(userId, "LOGOUT", "SUCCESS", sourceIp));
    }

    public List<AuthLog> getAuthLogs()        throws SQLException { return authLogDAO.getAll(); }
    public List<AuthLog> getFailedLogins()    throws SQLException { return authLogDAO.getFailedLogins(); }
    public List<AuthLog> getAuthLogsByUser(int uid) throws SQLException { return authLogDAO.getByUserId(uid); }

    // ================================================================
    //  PASSWORD EVENTS  — throws ValidationException
    // ================================================================

    public void changePassword(int userId, String oldPlain, String newPlain, String sourceIp)
            throws AuditException, SQLException {

        User user = userDAO.getUserById(userId);
        if (user == null) throw new UserNotFoundException(userId);

        if (!PasswordUtil.verify(oldPlain, user.getPasswordHash())) {
            passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "FAILURE"));
            validationLogDAO.insert(new InputValidationLog(
                    userId, "password", "Old password mismatch", sourceIp));
            throw new ValidationException("password", "***", "Current password is incorrect");
        }
        if (newPlain.length() < 6) {
            passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "FAILURE"));
            throw new ValidationException("new_password", "***",
                    "New password must be at least 6 characters");
        }
        if (newPlain.equals(oldPlain))
            throw new ValidationException("new_password", "***",
                    "New password must differ from the current password");

        userDAO.updatePassword(userId, PasswordUtil.hash(newPlain));
        passwordEventDAO.insert(new PasswordEvent(userId, "CHANGE", "SUCCESS"));
    }

    public void requestPasswordReset(int userId) throws AuditException, SQLException {
        User u = userDAO.getUserById(userId);
        if (u == null) throw new UserNotFoundException(userId);
        passwordEventDAO.insert(new PasswordEvent(userId, "RESET_REQUEST", "SUCCESS"));
    }

    public List<PasswordEvent> getPasswordEvents() throws SQLException {
        return passwordEventDAO.getAll();
    }

    // ================================================================
    //  SESSION MANAGEMENT
    // ================================================================

    public int openSession(int userId, String sourceIp) throws SQLException {
        Session s = new Session(userId, sourceIp);
        sessionDAO.createSession(s);
        return s.getSessionId();
    }

    public void terminateSession(int sessionId) throws AuditException, SQLException {
        boolean ok = sessionDAO.terminateSession(sessionId, "TERMINATED");
        if (!ok) throw new AuditException("SESSION_NOT_FOUND",
                "No active session found with ID: " + sessionId);
    }

    public List<Session> getAllSessions()    throws SQLException { return sessionDAO.getAll(); }
    public List<Session> getActiveSessions() throws SQLException { return sessionDAO.getActiveSessions(); }

    // ================================================================
    //  INPUT VALIDATION LOGS
    // ================================================================

    public void logValidationFailure(int userId, String field, String reason, String sourceIp)
            throws AuditException, SQLException {

        if (field == null || field.trim().isEmpty())
            throw new ValidationException("input_field", "", "Field name cannot be empty");
        if (reason == null || reason.trim().isEmpty())
            throw new ValidationException("failure_reason", "", "Failure reason cannot be empty");

        InputValidationLog log = new InputValidationLog(userId, field.trim(), reason.trim(), sourceIp);
        boolean ok = validationLogDAO.insert(log);
        if (!ok) throw new AuditException("LOG_FAILED", "Failed to insert validation log");
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

    // ================================================================
    //  ROLE GUARD  — throws AccessDeniedException
    // ================================================================

    /**
     * Called by menu before any privileged operation.
     * INHERITANCE: throws AccessDeniedException extends AuditException.
     */
    public void requireRole(User caller, String requiredRole) throws AccessDeniedException {
        if (caller == null)
            throw new AccessDeniedException(requiredRole, "NONE");
        if (!caller.getRole().equals(requiredRole) && !caller.getRole().equals("ADMIN"))
            throw new AccessDeniedException(requiredRole, caller.getRole());
    }

    // ================================================================
    //  STORED PROCEDURE WRAPPERS
    //  These call MySQL stored procedures via CallableStatement.
    // ================================================================

    /**
     * sp_register_user — registers user + logs USER_EVENT atomically in DB.
     * Replaces the plain registerUser() for the procedure-based flow.
     */
    public String registerUserViaProcedure(String username, String email,
                                           String plainPassword, String role,
                                           int performedBy)
            throws AuditException, SQLException {
        if (username == null || username.trim().isEmpty())
            throw new ValidationException("username", "", "Username cannot be empty");
        if (!email.contains("@") || !email.contains("."))
            throw new ValidationException("email", email, "Invalid email format");
        if (plainPassword.length() < 6)
            throw new ValidationException("password", "***", "Password must be at least 6 characters");
        if (!role.matches("ADMIN|ANALYST|USER"))
            throw new ValidationException("role", role, "Must be ADMIN, ANALYST, or USER");

        java.util.Map<String, Object> result = procedureDAO.registerUser(
                username.trim(), email.trim(),
                com.audit.util.PasswordUtil.hash(plainPassword),
                role, performedBy);

        String msg = (String) result.get("resultMsg");
        if (msg != null && msg.startsWith("ERROR"))
            throw new AuditException("PROC_ERR", msg);
        return msg + "  [via stored procedure sp_register_user]";
    }

    /**
     * sp_change_password — full validation + update + logging done in MySQL.
     */
    public void changePasswordViaProcedure(int userId, String oldPlain,
                                           String newPlain, String sourceIp)
            throws AuditException, SQLException {
        if (newPlain.length() < 6)
            throw new ValidationException("new_password", "***", "Must be at least 6 characters");

        java.util.Map<String, Object> result = procedureDAO.changePassword(
                userId,
                com.audit.util.PasswordUtil.hash(oldPlain),
                com.audit.util.PasswordUtil.hash(newPlain),
                sourceIp);

        boolean ok  = (Boolean) result.get("success");
        String  msg = (String)  result.get("resultMsg");
        if (!ok) throw new AuditException("PROC_ERR", msg);
    }

    /**
     * sp_get_user_audit_report — pulls all 5 log tables for a user in one call.
     */
    public java.util.Map<String, java.util.List<String[]>> getUserAuditReport(int userId)
            throws AuditException, SQLException {
        if (userDAO.getUserById(userId) == null)
            throw new UserNotFoundException(userId);
        return procedureDAO.getUserAuditReport(userId);
    }

    /**
     * sp_terminate_expired_sessions — bulk-expire sessions older than N hours.
     */
    public int terminateExpiredSessions(int hours) throws SQLException {
        return procedureDAO.terminateExpiredSessions(hours);
    }
}
