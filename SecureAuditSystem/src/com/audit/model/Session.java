package com.audit.model;

import java.time.LocalDateTime;

public class Session {
    private int    sessionId;
    private int    userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String sessionStatus;
    private String sourceIp;

    public Session() {}

    public Session(int userId, String sourceIp) {
        this.userId        = userId;
        this.sourceIp      = sourceIp;
        this.sessionStatus = "ACTIVE";
    }

    public int    getSessionId()                      { return sessionId; }
    public void   setSessionId(int id)                { this.sessionId = id; }
    public int    getUserId()                         { return userId; }
    public void   setUserId(int id)                   { this.userId = id; }
    public LocalDateTime getLoginTime()               { return loginTime; }
    public void   setLoginTime(LocalDateTime t)       { this.loginTime = t; }
    public LocalDateTime getLogoutTime()              { return logoutTime; }
    public void   setLogoutTime(LocalDateTime t)      { this.logoutTime = t; }
    public String getSessionStatus()                  { return sessionStatus; }
    public void   setSessionStatus(String s)          { this.sessionStatus = s; }
    public String getSourceIp()                       { return sourceIp; }
    public void   setSourceIp(String ip)              { this.sourceIp = ip; }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-10s | Login:%-20s | Logout:%-20s | %s",
                sessionId, userId, sessionStatus, loginTime, logoutTime, sourceIp);
    }
}
