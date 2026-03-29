package com.audit.model;

import java.time.LocalDateTime;

public class AuthLog {
    private int    authId;
    private int    userId;
    private String authType;
    private String authResult;
    private String sourceIp;
    private LocalDateTime timestamp;

    public AuthLog() {}

    public AuthLog(int userId, String authType, String authResult, String sourceIp) {
        this.userId     = userId;
        this.authType   = authType;
        this.authResult = authResult;
        this.sourceIp   = sourceIp;
    }

    public int    getAuthId()                     { return authId; }
    public void   setAuthId(int id)               { this.authId = id; }
    public int    getUserId()                     { return userId; }
    public void   setUserId(int id)               { this.userId = id; }
    public String getAuthType()                   { return authType; }
    public void   setAuthType(String t)           { this.authType = t; }
    public String getAuthResult()                 { return authResult; }
    public void   setAuthResult(String r)         { this.authResult = r; }
    public String getSourceIp()                   { return sourceIp; }
    public void   setSourceIp(String ip)          { this.sourceIp = ip; }
    public LocalDateTime getTimestamp()           { return timestamp; }
    public void   setTimestamp(LocalDateTime ts)  { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-7s | %-8s | %-15s | %s",
                authId, userId, authType, authResult, sourceIp, timestamp);
    }
}
