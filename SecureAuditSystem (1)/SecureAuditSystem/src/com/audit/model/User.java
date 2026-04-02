package com.audit.model;

import java.time.LocalDateTime;

public class User {
    private int    userId;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String email, String passwordHash, String role, String accountStatus) {
        this.username      = username;
        this.email         = email;
        this.passwordHash  = passwordHash;
        this.role          = role;
        this.accountStatus = accountStatus;
    }

    // ---- Getters & Setters ----
    public int    getUserId()        { return userId; }
    public void   setUserId(int id)  { this.userId = id; }

    public String getUsername()              { return username; }
    public void   setUsername(String u)      { this.username = u; }

    public String getEmail()                 { return email; }
    public void   setEmail(String e)         { this.email = e; }

    public String getPasswordHash()          { return passwordHash; }
    public void   setPasswordHash(String ph) { this.passwordHash = ph; }

    public String getRole()                  { return role; }
    public void   setRole(String r)          { this.role = r; }

    public String getAccountStatus()         { return accountStatus; }
    public void   setAccountStatus(String s) { this.accountStatus = s; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void          setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    @Override
    public String toString() {
        return String.format("%-4d | %-15s | %-25s | %-8s | %-10s | %s",
                userId, username, email, role, accountStatus, createdAt);
    }
}
