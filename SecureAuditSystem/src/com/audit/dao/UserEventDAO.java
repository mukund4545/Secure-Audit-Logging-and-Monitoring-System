package com.audit.dao;

import com.audit.model.UserEvent;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserEventDAO {

    public boolean insert(UserEvent ue) throws SQLException {
        String sql = "INSERT INTO USER_EVENT (target_user_id, performed_by, event_type) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ue.getTargetUserId());
            ps.setInt(2, ue.getPerformedBy());
            ps.setString(3, ue.getEventType());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) ue.setUserEventId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    public List<UserEvent> getAll() throws SQLException {
        List<UserEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM USER_EVENT ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<UserEvent> getByTargetUser(int userId) throws SQLException {
        List<UserEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM USER_EVENT WHERE target_user_id = ? ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private UserEvent map(ResultSet rs) throws SQLException {
        UserEvent ue = new UserEvent();
        ue.setUserEventId(rs.getInt("user_event_id"));
        ue.setTargetUserId(rs.getInt("target_user_id"));
        ue.setPerformedBy(rs.getInt("performed_by"));
        ue.setEventType(rs.getString("event_type"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) ue.setTimestamp(ts.toLocalDateTime());
        return ue;
    }
}
