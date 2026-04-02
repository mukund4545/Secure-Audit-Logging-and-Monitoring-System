package com.audit.dao;

import com.audit.interfaces.BaseDAO;
import com.audit.model.PasswordEvent;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** INTERFACE IMPLEMENTATION: implements BaseDAO<PasswordEvent> */
public class PasswordEventDAO implements BaseDAO<PasswordEvent> {

    @Override
    public boolean insert(PasswordEvent pe) throws SQLException {
        String sql = "INSERT INTO PASSWORD_EVENT (user_id, event_type, event_status) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pe.getUserId());
            ps.setString(2, pe.getEventType());
            ps.setString(3, pe.getEventStatus());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) pe.setPasswordEventId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<PasswordEvent> getAll() throws SQLException {
        List<PasswordEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM PASSWORD_EVENT ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public List<PasswordEvent> getByUserId(int userId) throws SQLException {
        List<PasswordEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM PASSWORD_EVENT WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private PasswordEvent map(ResultSet rs) throws SQLException {
        PasswordEvent p = new PasswordEvent();
        p.setPasswordEventId(rs.getInt("password_event_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setEventType(rs.getString("event_type"));
        p.setEventStatus(rs.getString("event_status"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) p.setTimestamp(ts.toLocalDateTime());
        return p;
    }
}
