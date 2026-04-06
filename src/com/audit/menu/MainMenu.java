package com.audit.menu;

import com.audit.exception.*;
import com.audit.model.*;
import com.audit.service.AuditService;
import com.audit.util.DBConnection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven terminal entry point.
 *
 * OOP concepts in action here:
 *  EXCEPTIONS   — every service call wrapped in try-catch for typed AuditException subtypes
 *  POLYMORPHISM — "View All Log Summaries" calls getSummary() across all BaseLog subtypes
 *  INHERITANCE  — catches AuditException (base) as fallback after specific subtype catches
 *  INTERFACE    — AuditService.requireRole() uses AccessDeniedException (extends AuditException)
 */
public class MainMenu {

    private static final AuditService service = new AuditService();
    private static final Scanner      scanner = new Scanner(System.in);

    private static User   currentUser = null;
    private static int    sessionId   = -1;
    private static final String CLIENT_IP = "127.0.0.1";

    public static void main(String[] args) {
        MenuHelper.printBanner();
        try {
            DBConnection.getConnection();
            MenuHelper.success("Connected to MySQL — secure_audit_db");
        } catch (SQLException e) {
            MenuHelper.error("Cannot connect to database: " + e.getMessage());
            MenuHelper.info("Ensure MySQL is running and sql/schema.sql has been executed.");
            return;
        }
        loginLoop();
        DBConnection.closeConnection();
    }

    // ================================================================
    //  LOGIN LOOP  — catches AuthException (user-defined)
    // ================================================================
    private static void loginLoop() {
        while (true) {
            MenuHelper.printHeader("LOGIN");
            System.out.print("Username (or 'exit'): ");
            String uname = scanner.nextLine().trim();
            if (uname.equalsIgnoreCase("exit")) { System.out.println("Goodbye!"); break; }
            System.out.print("Password            : ");
            String pass = scanner.nextLine().trim();

            try {
                currentUser = service.login(uname, pass, CLIENT_IP);
                MenuHelper.success("Welcome, " + currentUser.getUsername() +
                                   "  [" + currentUser.getRole() + "]");
                sessionId = service.openSession(currentUser.getUserId(), CLIENT_IP);
                mainMenuLoop();
                currentUser = null;
                sessionId   = -1;

            } catch (AuthException e) {
                // TYPED catch — AuthException (user-defined, extends AuditException)
                MenuHelper.error("Login failed: " + e.getMessage());
                MenuHelper.info("Reason code: " + e.getErrorCode() +
                                " | Type: " + e.getReason());
            } catch (SQLException e) {
                MenuHelper.error("Database error during login: " + e.getMessage());
            }
        }
    }

    // ================================================================
    //  MAIN MENU
    // ================================================================
    private static void mainMenuLoop() {
        while (true) {
            MenuHelper.printHeader("MAIN MENU  [" + currentUser.getUsername() +
                                   " | " + currentUser.getRole() + "]");
            System.out.println("  1.  User Management");
            System.out.println("  2.  Authentication Logs");
            System.out.println("  3.  Password Events");
            System.out.println("  4.  Session Management");
            System.out.println("  5.  Input Validation Logs");
            System.out.println("  6.  User Event Audit Trail");
            System.out.println("  7.  Change My Password");
            // System.out.println("  8.  Triggers & Stored Procedures");
            System.out.println("  0.  Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1":  userManagementMenu();   break;
                case "2":  authLogMenu();          break;
                case "3":  passwordEventMenu();    break;
                case "4":  sessionMenu();          break;
                case "5":  validationLogMenu();    break;
                case "6":  userEventMenu();        break;
                case "7":  changeMyPassword();     break;
                // case "8":  triggerProcedureMenu();  break;
                case "0":
                    try {
                        service.recordLogout(currentUser.getUserId(), CLIENT_IP);
                        service.terminateSession(sessionId);
                    } catch (AuditException | SQLException ex) {
                        MenuHelper.error("Logout error: " + ex.getMessage());
                    }
                    MenuHelper.info("Logged out. Session terminated.");
                    return;
                default: MenuHelper.error("Invalid option.");
            }
        }
    }

    // ================================================================
    //  MODULE 8 — TRIGGERS & STORED PROCEDURES
    // ================================================================
    // private static void triggerProcedureMenu() {
    //     while (true) {
    //         MenuHelper.printHeader("TRIGGERS & STORED PROCEDURES");
    //         System.out.println("  ── Stored Procedures ─────────────────────────────");
    //         System.out.println("  1. Register User            (sp_register_user)");
    //         System.out.println("  2. Change Password          (sp_change_password)");
    //         System.out.println("  3. Full User Audit Report   (sp_get_user_audit_report)");
    //         System.out.println("  4. Expire Old Sessions      (sp_terminate_expired_sessions)");
    //         System.out.println("  ── Triggers (auto-fire info) ─────────────────────");
    //         System.out.println("  5. Explain Active Triggers");
    //         System.out.println("  0. Back");
    //         System.out.print("Choose: ");

    //         switch (scanner.nextLine().trim()) {
    //             case "1": procRegisterUser();        break;
    //             case "2": procChangePassword();      break;
    //             case "3": procAuditReport();         break;
    //             case "4": procExpireSessions();      break;
    //             case "5": showTriggerInfo();         break;
    //             case "0": return;
    //             default:  MenuHelper.error("Invalid option.");
    //         }
    //     }
    // }

    // ── Stored Procedure: sp_register_user ──────────────────
    private static void procRegisterUser() {
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        MenuHelper.printHeader("REGISTER USER  [via sp_register_user]");
        System.out.print("Username   : "); String username = scanner.nextLine().trim();
        System.out.print("Email      : "); String email    = scanner.nextLine().trim();
        System.out.print("Password   : "); String pass     = scanner.nextLine().trim();
        System.out.print("Role (ADMIN/ANALYST/USER): ");
        String role = scanner.nextLine().trim().toUpperCase();

        try {
            String msg = service.registerUserViaProcedure(
                    username, email, pass, role, currentUser.getUserId());
            MenuHelper.success(msg);
        } catch (ValidationException e) {
            MenuHelper.error("Validation — " + e.getFieldName() + ": " + e.getMessage());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    // ── Stored Procedure: sp_change_password ────────────────
    private static void procChangePassword() {
        MenuHelper.printHeader("CHANGE PASSWORD  [via sp_change_password]");
        MenuHelper.info("All validation, update and logging handled inside MySQL procedure.");
        System.out.print("Current Password : "); String oldPass = scanner.nextLine().trim();
        System.out.print("New Password     : "); String newPass = scanner.nextLine().trim();
        System.out.print("Confirm New Pass : "); String confirm = scanner.nextLine().trim();

        if (!newPass.equals(confirm)) { MenuHelper.error("Passwords do not match."); return; }

        try {
            service.changePasswordViaProcedure(
                    currentUser.getUserId(), oldPass, newPass, CLIENT_IP);
            MenuHelper.success("Password changed via stored procedure sp_change_password.");
        } catch (ValidationException e) {
            MenuHelper.error("Validation failed: " + e.getMessage());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    // ── Stored Procedure: sp_get_user_audit_report ──────────
    private static void procAuditReport() {
        System.out.print("User ID for audit report: "); int uid = readInt();

        try {
            java.util.Map<String, java.util.List<String[]>> report =
                    service.getUserAuditReport(uid);

            MenuHelper.printHeader("USER AUDIT REPORT  [via sp_get_user_audit_report]");
            MenuHelper.info("5 result sets returned from a single stored procedure call.");

            for (java.util.Map.Entry<String, java.util.List<String[]>> entry : report.entrySet()) {
                System.out.println();
                MenuHelper.tableHeader("  [ " + entry.getKey().toUpperCase() + " ]");
                java.util.List<String[]> rows = entry.getValue();
                if (rows.size() <= 1) { MenuHelper.noData(); continue; }
                // Print header row
                System.out.println("  " + String.join(" | ", rows.get(0)));
                System.out.println("  " + "-".repeat(60));
                // Print data rows
                for (int i = 1; i < rows.size(); i++) {
                    System.out.println("  " + String.join(" | ", rows.get(i)));
                }
            }
        } catch (UserNotFoundException e) {
            MenuHelper.error(e.getMessage());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    // ── Stored Procedure: sp_terminate_expired_sessions ─────
    private static void procExpireSessions() {
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        MenuHelper.printHeader("EXPIRE OLD SESSIONS  [via sp_terminate_expired_sessions]");
        System.out.print("Expire sessions older than how many hours? (e.g. 8): ");
        int hours = readInt();
        if (hours <= 0) { MenuHelper.error("Hours must be a positive number."); return; }

        try {
            int count = service.terminateExpiredSessions(hours);
            if (count == 0)
                MenuHelper.info("No active sessions older than " + hours + " hour(s) found.");
            else
                MenuHelper.success(count + " session(s) marked EXPIRED by stored procedure.");
        } catch (SQLException e) {
            MenuHelper.error("DB error: " + e.getMessage());
        }
    }

    // ── Trigger Info ─────────────────────────────────────────
    private static void showTriggerInfo() {
        MenuHelper.printHeader("ACTIVE TRIGGERS  [fire automatically in MySQL]");
        System.out.println();
        System.out.println("  1. trg_auto_lock_after_failed_logins");
        System.out.println("     Fires : AFTER INSERT on AUTHENTICATION");
        System.out.println("     Action: If a user gets 5 FAILURE logins in 10 minutes,");
        System.out.println("             their account is automatically LOCKED in USER table.");
        System.out.println("             A LOCKED entry is also added to USER_EVENT.");
        System.out.println("     Test  : Use 'Simulate Login Attempt' with wrong password 5 times.");
        System.out.println();
        System.out.println("  2. trg_log_new_user_created");
        System.out.println("     Fires : AFTER INSERT on USER");
        System.out.println("     Action: Auto-inserts a CREATED record in USER_EVENT.");
        System.out.println("             Guarantees audit trail even if Java forgets to log it.");
        System.out.println();
        System.out.println("  3. trg_log_user_status_change");
        System.out.println("     Fires : AFTER UPDATE on USER");
        System.out.println("     Action: If account_status or role changes, auto-logs to USER_EVENT.");
        System.out.println("             Catches changes made directly in MySQL Workbench too.");
        System.out.println();
        System.out.println("  4. trg_prevent_last_admin_delete");
        System.out.println("     Fires : BEFORE DELETE on USER");
        System.out.println("     Action: If deleting an ADMIN would leave zero admins,");
        System.out.println("             the DELETE is CANCELLED with a SQLSTATE 45000 error.");
        System.out.println("     Test  : Try deleting 'admin' when it's the only admin.");
        System.out.println();
    }

    // ================================================================
    //  MODULE 1 — USER MANAGEMENT
    // ================================================================
    private static void userManagementMenu() {
        while (true) {
            MenuHelper.printHeader("USER MANAGEMENT");
            System.out.println("  1. List All Users");
            System.out.println("  2. Create New User        [ADMIN]");
            System.out.println("  3. Change User Role       [ADMIN]");
            System.out.println("  4. Change User Status     [ADMIN]");
            System.out.println("  5. Delete User            [ADMIN]");
            System.out.println("  6. View User by ID");
            System.out.println("  0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1": listAllUsers();   break;
                case "2": createUser();     break;
                case "3": changeRole();     break;
                case "4": changeStatus();   break;
                case "5": deleteUser();     break;
                case "6": viewUserById();   break;
                case "0": return;
                default:  MenuHelper.error("Invalid option.");
            }
        }
    }

    private static void listAllUsers() {
        try {
            List<User> users = service.listAllUsers();
            MenuHelper.printHeader("ALL USERS");
            if (users.isEmpty()) { MenuHelper.noData(); return; }
            MenuHelper.tableHeader(String.format("%-4s | %-15s | %-25s | %-8s | %-10s | %s",
                    "ID","USERNAME","EMAIL","ROLE","STATUS","CREATED_AT"));
            users.forEach(System.out::println);
        } catch (SQLException e) {
            MenuHelper.error("DB Error: " + e.getMessage());
        }
    }

    private static void viewUserById() {
        System.out.print("User ID: "); int id = readInt();
        try {
            User u = service.getUserById(id);
            MenuHelper.printHeader("USER DETAILS — ID " + id);
            System.out.println(u);
        } catch (UserNotFoundException e) {
            // TYPED catch — UserNotFoundException (user-defined, extends AuditException)
            MenuHelper.error(e.getMessage() + "  [Code: " + e.getErrorCode() + "]");
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    private static void createUser() {
        // AccessDeniedException (user-defined) thrown if not ADMIN
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        MenuHelper.printHeader("CREATE USER");
        System.out.print("Username   : "); String username = scanner.nextLine().trim();
        System.out.print("Email      : "); String email    = scanner.nextLine().trim();
        System.out.print("Password   : "); String pass     = scanner.nextLine().trim();
        System.out.print("Role (ADMIN/ANALYST/USER): ");
        String role = scanner.nextLine().trim().toUpperCase();

        try {
            User created = service.registerUser(username, email, pass, role,
                                                currentUser.getUserId());
            MenuHelper.success("User '" + created.getUsername() +
                               "' created with ID " + created.getUserId());
        } catch (ValidationException e) {
            // TYPED catch — ValidationException (user-defined)
            MenuHelper.error("Validation failed — Field: '" + e.getFieldName() +
                             "' | Value: '" + e.getRejectedValue() + "'");
            MenuHelper.error("Reason: " + e.getMessage());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    private static void changeRole() {
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        System.out.print("Target User ID: "); int tid = readInt();
        System.out.print("New Role (ADMIN/ANALYST/USER): ");
        String role = scanner.nextLine().trim().toUpperCase();

        try {
            service.changeUserRole(tid, role, currentUser.getUserId());
            MenuHelper.success("Role updated to " + role + " for user " + tid);
        } catch (UserNotFoundException e) {
            MenuHelper.error(e.getMessage());
        } catch (ValidationException e) {
            MenuHelper.error("Invalid role value: " + e.getRejectedValue());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    private static void changeStatus() {
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        System.out.print("Target User ID: "); int tid = readInt();
        System.out.print("New Status (ACTIVE/LOCKED/INACTIVE/SUSPENDED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        try {
            service.changeUserStatus(tid, status, currentUser.getUserId());
            MenuHelper.success("Status updated to " + status + " for user " + tid);
        } catch (UserNotFoundException e) {
            MenuHelper.error(e.getMessage());
        } catch (ValidationException e) {
            MenuHelper.error("Invalid status value: " + e.getRejectedValue());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    private static void deleteUser() {
        try { service.requireRole(currentUser, "ADMIN"); }
        catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); return; }

        System.out.print("User ID to delete: "); int tid = readInt();
        if (tid == currentUser.getUserId()) {
            MenuHelper.error("You cannot delete your own account.");
            return;
        }
        System.out.print("Confirm delete? (yes/no): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            MenuHelper.info("Deletion cancelled.");
            return;
        }
        try {
            service.deleteUser(tid, currentUser.getUserId());
            MenuHelper.success("User " + tid + " deleted successfully.");
        } catch (UserNotFoundException e) {
            MenuHelper.error(e.getMessage());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    // ================================================================
    //  MODULE 2 — AUTHENTICATION LOGS
    // ================================================================
    private static void authLogMenu() {
        while (true) {
            MenuHelper.printHeader("AUTHENTICATION LOGS");
            System.out.println("  1. View All Auth Logs");
            System.out.println("  2. View Failed Login Attempts");
            System.out.println("  3. View Auth Logs by User ID");
            System.out.println("  4. Simulate Login Attempt");
            System.out.println("  0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1": viewAllAuthLogs();   break;
                case "2": viewFailedLogins();  break;
                case "3": viewAuthByUser();    break;
                case "4": simulateLogin();     break;
                case "0": return;
                default:  MenuHelper.error("Invalid option.");
            }
        }
    }

    private static void viewAllAuthLogs() {
        try {
            List<AuthLog> logs = service.getAuthLogs();
            MenuHelper.printHeader("ALL AUTHENTICATION LOGS");
            if (logs.isEmpty()) { MenuHelper.noData(); return; }
            MenuHelper.tableHeader(String.format("%-5s | %-6s | %-7s | %-8s | %-15s | %s",
                    "ID","UID","TYPE","RESULT","SOURCE_IP","TIMESTAMP"));
            logs.forEach(System.out::println);
        } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
    }

    private static void viewFailedLogins() {
        try {
            List<AuthLog> logs = service.getFailedLogins();
            MenuHelper.printHeader("FAILED LOGIN ATTEMPTS");
            if (logs.isEmpty()) { MenuHelper.noData(); return; }
            MenuHelper.tableHeader(String.format("%-5s | %-6s | %-7s | %-8s | %-15s | %s",
                    "ID","UID","TYPE","RESULT","SOURCE_IP","TIMESTAMP"));
            logs.forEach(System.out::println);
            MenuHelper.info("Total failed attempts: " + logs.size());
        } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
    }

    private static void viewAuthByUser() {
        System.out.print("User ID: "); int uid = readInt();
        try {
            List<AuthLog> logs = service.getAuthLogsByUser(uid);
            MenuHelper.printHeader("AUTH LOGS FOR USER " + uid);
            if (logs.isEmpty()) { MenuHelper.noData(); return; }
            logs.forEach(System.out::println);
        } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
    }

    private static void simulateLogin() {
        MenuHelper.printHeader("SIMULATE LOGIN ATTEMPT");
        MenuHelper.info("This demonstrates AuthException being caught with typed reason codes.");
        System.out.print("Username : "); String u  = scanner.nextLine().trim();
        System.out.print("Password : "); String p  = scanner.nextLine().trim();
        System.out.print("Source IP: "); String ip = scanner.nextLine().trim();

        try {
            User result = service.login(u, p, ip.isEmpty() ? CLIENT_IP : ip);
            MenuHelper.success("Login SUCCESS for user: " + result.getUsername() +
                               " [" + result.getRole() + "]");
        } catch (AuthException e) {
            // TYPED catch per reason
            switch (e.getReason()) {
                case INVALID_CREDENTIALS:
                    MenuHelper.error("Wrong password. " + e.getMessage()); break;
                case ACCOUNT_LOCKED:
                    MenuHelper.error("Account is LOCKED. " + e.getMessage()); break;
                case ACCOUNT_INACTIVE:
                    MenuHelper.error("Account is INACTIVE. " + e.getMessage()); break;
                case ACCOUNT_SUSPENDED:
                    MenuHelper.error("Account is SUSPENDED. " + e.getMessage()); break;
                case USER_NOT_FOUND:
                    MenuHelper.error("Username not found. " + e.getMessage()); break;
            }
            MenuHelper.info("This attempt has been logged in the AUTHENTICATION table.");
        } catch (SQLException e) {
            MenuHelper.error("DB error: " + e.getMessage());
        }
    }

    // ================================================================
    //  MODULE 3 — PASSWORD EVENTS
    // ================================================================
    private static void passwordEventMenu() {
        while (true) {
            MenuHelper.printHeader("PASSWORD EVENTS");
            System.out.println("  1. View All Password Events");
            System.out.println("  2. Request Password Reset (Log)");
            System.out.println("  0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    try {
                        List<PasswordEvent> evs = service.getPasswordEvents();
                        MenuHelper.printHeader("PASSWORD EVENT LOG");
                        if (evs.isEmpty()) { MenuHelper.noData(); break; }
                        MenuHelper.tableHeader(String.format("%-5s | %-6s | %-16s | %-8s | %s",
                                "ID","UID","EVENT_TYPE","STATUS","TIMESTAMP"));
                        evs.forEach(System.out::println);
                    } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
                    break;
                case "2":
                    System.out.print("User ID for reset: "); int uid = readInt();
                    try {
                        service.requestPasswordReset(uid);
                        MenuHelper.success("Password reset request logged for user " + uid);
                    } catch (UserNotFoundException e) {
                        MenuHelper.error(e.getMessage());
                    } catch (AuditException | SQLException e) {
                        MenuHelper.error(e.getMessage());
                    }
                    break;
                case "0": return;
                default:  MenuHelper.error("Invalid option.");
            }
        }
    }

    // ================================================================
    //  MODULE 4 — SESSION MANAGEMENT
    // ================================================================
    private static void sessionMenu() {
        while (true) {
            MenuHelper.printHeader("SESSION MANAGEMENT");
            System.out.println("  1. View All Sessions");
            System.out.println("  2. View Active Sessions");
            System.out.println("  3. Terminate a Session    [ADMIN]");
            System.out.println("  0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    try {
                        List<Session> ss = service.getAllSessions();
                        MenuHelper.printHeader("ALL SESSIONS");
                        if (ss.isEmpty()) { MenuHelper.noData(); break; }
                        MenuHelper.tableHeader(String.format("%-5s | %-6s | %-10s | %-22s | %-22s | %s",
                                "SID","UID","STATUS","LOGIN_TIME","LOGOUT_TIME","IP"));
                        ss.forEach(System.out::println);
                    } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
                    break;
                case "2":
                    try {
                        List<Session> ss = service.getActiveSessions();
                        MenuHelper.printHeader("ACTIVE SESSIONS");
                        if (ss.isEmpty()) { MenuHelper.noData(); break; }
                        ss.forEach(System.out::println);
                    } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
                    break;
                case "3":
                    try { service.requireRole(currentUser, "ADMIN"); }
                    catch (AccessDeniedException e) { MenuHelper.error(e.getMessage()); break; }
                    System.out.print("Session ID to terminate: "); int sid = readInt();
                    try {
                        service.terminateSession(sid);
                        MenuHelper.success("Session " + sid + " terminated.");
                    } catch (AuditException | SQLException e) {
                        MenuHelper.error(e.getMessage());
                    }
                    break;
                case "0": return;
                default:  MenuHelper.error("Invalid option.");
            }
        }
    }

    // ================================================================
    //  MODULE 5 — INPUT VALIDATION LOGS
    // ================================================================
    private static void validationLogMenu() {
        while (true) {
            MenuHelper.printHeader("INPUT VALIDATION LOGS");
            System.out.println("  1. View All Validation Failures");
            System.out.println("  2. Log a New Validation Failure");
            System.out.println("  0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    try {
                        List<InputValidationLog> logs = service.getValidationLogs();
                        MenuHelper.printHeader("VALIDATION FAILURE LOG");
                        if (logs.isEmpty()) { MenuHelper.noData(); break; }
                        MenuHelper.tableHeader(String.format("%-5s | %-6s | %-15s | %-30s | %-15s | %s",
                                "ID","UID","FIELD","REASON","IP","TIMESTAMP"));
                        logs.forEach(System.out::println);
                    } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
                    break;
                case "2":
                    MenuHelper.printHeader("LOG VALIDATION FAILURE");
                    System.out.print("User ID (0=unknown) : "); int uid    = readInt();
                    System.out.print("Input Field         : "); String field  = scanner.nextLine().trim();
                    System.out.print("Failure Reason      : "); String reason = scanner.nextLine().trim();
                    System.out.print("Source IP           : "); String ip     = scanner.nextLine().trim();
                    try {
                        service.logValidationFailure(uid, field, reason,
                                ip.isEmpty() ? CLIENT_IP : ip);
                        MenuHelper.success("Validation failure logged successfully.");
                    } catch (ValidationException e) {
                        // TYPED catch — ValidationException (user-defined)
                        MenuHelper.error("Bad input — Field: " + e.getFieldName() +
                                         " | " + e.getMessage());
                    } catch (AuditException | SQLException e) {
                        MenuHelper.error(e.getMessage());
                    }
                    break;
                case "0": return;
                default:  MenuHelper.error("Invalid option.");
            }
        }
    }

    // ================================================================
    //  MODULE 6 — USER EVENT AUDIT TRAIL
    // ================================================================
    private static void userEventMenu() {
        try {
            List<UserEvent> evs = service.getUserEvents();
            MenuHelper.printHeader("USER EVENT AUDIT TRAIL");
            if (evs.isEmpty()) { MenuHelper.noData(); return; }
            MenuHelper.tableHeader(String.format("%-5s | %-12s | %-8s | %-12s | %s",
                    "ID","TARGET_UID","BY_UID","EVENT_TYPE","TIMESTAMP"));
            evs.forEach(System.out::println);
        } catch (SQLException e) { MenuHelper.error(e.getMessage()); }
    }

    // ================================================================
    //  CHANGE MY PASSWORD — ValidationException
    // ================================================================
    private static void changeMyPassword() {
        MenuHelper.printHeader("CHANGE MY PASSWORD");
        System.out.print("Current Password : "); String oldPass = scanner.nextLine().trim();
        System.out.print("New Password     : "); String newPass = scanner.nextLine().trim();
        System.out.print("Confirm New Pass : "); String confirm = scanner.nextLine().trim();

        if (!newPass.equals(confirm)) {
            MenuHelper.error("Passwords do not match. No changes made.");
            return;
        }
        try {
            service.changePassword(currentUser.getUserId(), oldPass, newPass, CLIENT_IP);
            MenuHelper.success("Password changed successfully.");
        } catch (ValidationException e) {
            // TYPED catch — ValidationException
            MenuHelper.error("Password change failed — " + e.getMessage());
            MenuHelper.info("Field: " + e.getFieldName());
        } catch (AuditException | SQLException e) {
            MenuHelper.error(e.getMessage());
        }
    }

    // ================================================================
    //  HELPER
    // ================================================================
    private static int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            MenuHelper.error("Invalid number. Defaulting to 0.");
            return 0;
        }
    }
}
