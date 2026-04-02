package com.audit.dao;

import com.audit.model.User;
import com.audit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // ---------- CREATE ----------
    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO USER (username, email, password_hash, role, account_status) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getAccountStatus());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) user.setUserId(rs.getInt(1));
                return true;
            }
        }
        return false;
    }

    // ---------- READ ALL ----------
    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM USER ORDER BY user_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ---------- READ BY ID ----------
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM USER WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    // ---------- READ BY USERNAME ----------
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM USER WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    // ---------- UPDATE ROLE ----------
    public boolean updateRole(int userId, String newRole) throws SQLException {
        String sql = "UPDATE USER SET role = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- UPDATE STATUS ----------
    public boolean updateStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE USER SET account_status = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- UPDATE PASSWORD ----------
    public boolean updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE USER SET password_hash = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- DELETE ----------
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM USER WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- MAPPER ----------
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setAccountStatus(rs.getString("account_status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
