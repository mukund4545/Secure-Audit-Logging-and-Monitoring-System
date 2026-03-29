package com.audit.dao;

import com.audit.model.Session;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {

    public boolean createSession(Session s) throws SQLException {
        String sql = "INSERT INTO SESSION (user_id, session_status, source_ip) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getSessionStatus());
            ps.setString(3, s.getSourceIp());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) s.setSessionId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    public boolean terminateSession(int sessionId, String status) throws SQLException {
        String sql = "UPDATE SESSION SET logout_time = NOW(), session_status = ? WHERE session_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Session> getAll() throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM SESSION ORDER BY login_time DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Session> getActiveSessions() throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM SESSION WHERE session_status = 'ACTIVE' ORDER BY login_time DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Session> getByUser(int userId) throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM SESSION WHERE user_id = ? ORDER BY login_time DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Session map(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setSessionId(rs.getInt("session_id"));
        s.setUserId(rs.getInt("user_id"));
        Timestamp lt = rs.getTimestamp("login_time");
        if (lt != null) s.setLoginTime(lt.toLocalDateTime());
        Timestamp lo = rs.getTimestamp("logout_time");
        if (lo != null) s.setLogoutTime(lo.toLocalDateTime());
        s.setSessionStatus(rs.getString("session_status"));
        s.setSourceIp(rs.getString("source_ip"));
        return s;
    }
}
