package com.audit.dao;

import com.audit.interfaces.BaseDAO;
import com.audit.model.AuthLog;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERFACE IMPLEMENTATION: implements BaseDAO<AuthLog>
 * POLYMORPHISM: insert/getAll/getByUserId called generically via BaseDAO<?>
 */
public class AuthLogDAO implements BaseDAO<AuthLog> {

    @Override
    public boolean insert(AuthLog log) throws SQLException {
        String sql = "INSERT INTO AUTHENTICATION (user_id, auth_type, auth_result, source_ip) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUserId() == 0) ps.setNull(1, Types.INTEGER);
            else                      ps.setInt(1, log.getUserId());
            ps.setString(2, log.getAuthType());
            ps.setString(3, log.getAuthResult());
            ps.setString(4, log.getSourceIp());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) log.setAuthId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<AuthLog> getAll() throws SQLException {
        List<AuthLog> list = new ArrayList<>();
        String sql = "SELECT * FROM AUTHENTICATION ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public List<AuthLog> getByUserId(int userId) throws SQLException {
        List<AuthLog> list = new ArrayList<>();
        String sql = "SELECT * FROM AUTHENTICATION WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<AuthLog> getFailedLogins() throws SQLException {
        List<AuthLog> list = new ArrayList<>();
        String sql = "SELECT * FROM AUTHENTICATION WHERE auth_result='FAILURE' ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private AuthLog map(ResultSet rs) throws SQLException {
        AuthLog a = new AuthLog();
        a.setAuthId(rs.getInt("auth_id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAuthType(rs.getString("auth_type"));
        a.setAuthResult(rs.getString("auth_result"));
        a.setSourceIp(rs.getString("source_ip"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) a.setTimestamp(ts.toLocalDateTime());
        return a;
    }
}
