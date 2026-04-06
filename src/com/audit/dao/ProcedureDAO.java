package com.audit.dao;

import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for all Stored Procedure calls.
 * Uses CallableStatement — the JDBC way to call MySQL procedures.
 *
 * Syntax:  { CALL procedure_name(?, ?, ...) }
 * IN  params: set with ps.setXxx(index, value)
 * OUT params: register with ps.registerOutParameter(index, Types.Xxx)
 *             read with ps.getXxx(index) after execute()
 */
public class ProcedureDAO {

    // ----------------------------------------------------------
    // CALL sp_register_user
    // ----------------------------------------------------------
    public Map<String, Object> registerUser(String username, String email,
                                             String passwordHash, String role,
                                             int performedBy) throws SQLException {
        String sql = "{ CALL sp_register_user(?, ?, ?, ?, ?, ?, ?) }";
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            // IN params
            cs.setString(1, username);
            cs.setString(2, email);
            cs.setString(3, passwordHash);
            cs.setString(4, role);
            cs.setInt(5, performedBy);

            // OUT params
            cs.registerOutParameter(6, Types.INTEGER); // p_new_user_id
            cs.registerOutParameter(7, Types.VARCHAR); // p_result_msg

            cs.execute();

            result.put("newUserId",  cs.getInt(6));
            result.put("resultMsg",  cs.getString(7));
        }
        return result;
    }

    // ----------------------------------------------------------
    // CALL sp_change_password
    // ----------------------------------------------------------
    public Map<String, Object> changePassword(int userId, String oldHash,
                                               String newHash, String sourceIp)
            throws SQLException {
        String sql = "{ CALL sp_change_password(?, ?, ?, ?, ?, ?) }";
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, userId);
            cs.setString(2, oldHash);
            cs.setString(3, newHash);
            cs.setString(4, sourceIp);
            cs.registerOutParameter(5, Types.TINYINT); // p_success
            cs.registerOutParameter(6, Types.VARCHAR); // p_result_msg

            cs.execute();

            result.put("success",   cs.getInt(5) == 1);
            result.put("resultMsg", cs.getString(6));
        }
        return result;
    }

    // ----------------------------------------------------------
    // CALL sp_get_user_audit_report
    // Returns a map with keys: profile, authLogs, passwordEvents,
    //                          sessions, validationLogs
    // Each value is a List<String[]> (rows of column values).
    // ----------------------------------------------------------
    public Map<String, List<String[]>> getUserAuditReport(int userId) throws SQLException {
        String sql = "{ CALL sp_get_user_audit_report(?) }";
        Map<String, List<String[]>> report = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, userId);
            boolean hasResults = cs.execute();

            // Result Set 1 — User profile
            report.put("profile",          readResultSet(cs.getResultSet()));
            cs.getMoreResults();
            // Result Set 2 — Auth logs
            report.put("authLogs",         readResultSet(cs.getResultSet()));
            cs.getMoreResults();
            // Result Set 3 — Password events
            report.put("passwordEvents",   readResultSet(cs.getResultSet()));
            cs.getMoreResults();
            // Result Set 4 — Sessions
            report.put("sessions",         readResultSet(cs.getResultSet()));
            cs.getMoreResults();
            // Result Set 5 — Validation logs
            report.put("validationLogs",   readResultSet(cs.getResultSet()));
        }
        return report;
    }

    // ----------------------------------------------------------
    // CALL sp_terminate_expired_sessions
    // ----------------------------------------------------------
    public int terminateExpiredSessions(int hoursThreshold) throws SQLException {
        String sql = "{ CALL sp_terminate_expired_sessions(?, ?) }";

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setInt(1, hoursThreshold);
            cs.registerOutParameter(2, Types.INTEGER); // p_expired_count

            cs.execute();
            return cs.getInt(2);
        }
    }

    // ----------------------------------------------------------
    // CALL sp_login_attempt
    // ----------------------------------------------------------
    public Map<String, Object> loginAttempt(String username,
                                             String authResult,
                                             String sourceIp) throws SQLException {
        String sql = "{ CALL sp_login_attempt(?, ?, ?, ?, ?, ?) }";
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, username);
            cs.setString(2, authResult);
            cs.setString(3, sourceIp);
            cs.registerOutParameter(4, Types.INTEGER); // p_user_id
            cs.registerOutParameter(5, Types.VARCHAR); // p_role
            cs.registerOutParameter(6, Types.VARCHAR); // p_status

            cs.execute();

            int uid = cs.getInt(4);
            result.put("userId", cs.wasNull() ? null : uid);
            result.put("role",   cs.getString(5));
            result.put("status", cs.getString(6));
        }
        return result;
    }

    // ----------------------------------------------------------
    // Helper: drain a ResultSet into List<String[]>
    // ----------------------------------------------------------
    private List<String[]> readResultSet(ResultSet rs) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        if (rs == null) return rows;
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        // First row = column headers
        String[] headers = new String[colCount];
        for (int i = 1; i <= colCount; i++) headers[i - 1] = meta.getColumnName(i);
        rows.add(headers);

        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                row[i - 1] = val != null ? val.toString() : "NULL";
            }
            rows.add(row);
        }
        rs.close();
        return rows;
    }
}
