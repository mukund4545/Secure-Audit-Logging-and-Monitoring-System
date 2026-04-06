package com.audit.model;

import java.time.LocalDateTime;

/**
 * INHERITANCE: extends BaseLog
 * POLYMORPHISM: overrides getSummary(), getCategory(), getLogId()
 */
public class Session extends BaseLog {

    private int           sessionId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String        sessionStatus;
    private String        sourceIp;

    public Session() {}

    public Session(int userId, String sourceIp) {
        this.userId        = userId;
        this.sourceIp      = sourceIp;
        this.sessionStatus = "ACTIVE";
    }

    public int    getSessionId()               { return sessionId; }
    public void   setSessionId(int id)         { this.sessionId = id; }
    public LocalDateTime getLoginTime()        { return loginTime; }
    public void   setLoginTime(LocalDateTime t){ this.loginTime = t; }
    public LocalDateTime getLogoutTime()       { return logoutTime; }
    public void   setLogoutTime(LocalDateTime t){ this.logoutTime = t; }
    public String getSessionStatus()           { return sessionStatus; }
    public void   setSessionStatus(String s)   { this.sessionStatus = s; }
    public String getSourceIp()                { return sourceIp; }
    public void   setSourceIp(String ip)       { this.sourceIp = ip; }

    @Override public int    getLogId()    { return sessionId; }
    @Override public String getCategory(){ return "SESSION"; }

    @Override
    public String getSummary() {
        return String.format("[SESSION] User:%d | %s | IP:%s | Login:%s",
                userId, sessionStatus, sourceIp,
                loginTime != null ? loginTime.format(FMT) : "N/A");
    }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-10s | Login:%-20s | Logout:%-20s | %s",
                sessionId, userId, sessionStatus,
                loginTime  != null ? loginTime.format(FMT)  : "N/A",
                logoutTime != null ? logoutTime.format(FMT) : "N/A",
                sourceIp);
    }
}
