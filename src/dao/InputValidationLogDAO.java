package com.audit.dao;

import com.audit.interfaces.BaseDAO;
import com.audit.model.InputValidationLog;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** INTERFACE IMPLEMENTATION: implements BaseDAO<InputValidationLog> */
public class InputValidationLogDAO implements BaseDAO<InputValidationLog> {

    @Override
    public boolean insert(InputValidationLog log) throws SQLException {
        String sql = "INSERT INTO INPUT_VALIDATION_LOG (user_id, input_field, failure_reason, source_ip) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUserId() == 0) ps.setNull(1, Types.INTEGER);
            else                      ps.setInt(1, log.getUserId());
            ps.setString(2, log.getInputField());
            ps.setString(3, log.getFailureReason());
            ps.setString(4, log.getSourceIp());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) log.setValidationLogId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<InputValidationLog> getAll() throws SQLException {
        List<InputValidationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM INPUT_VALIDATION_LOG ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public List<InputValidationLog> getByUserId(int userId) throws SQLException {
        List<InputValidationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM INPUT_VALIDATION_LOG WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private InputValidationLog map(ResultSet rs) throws SQLException {
        InputValidationLog l = new InputValidationLog();
        l.setValidationLogId(rs.getInt("validation_log_id"));
        l.setUserId(rs.getInt("user_id"));
        l.setInputField(rs.getString("input_field"));
        l.setFailureReason(rs.getString("failure_reason"));
        l.setSourceIp(rs.getString("source_ip"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) l.setTimestamp(ts.toLocalDateTime());
        return l;
    }
}
