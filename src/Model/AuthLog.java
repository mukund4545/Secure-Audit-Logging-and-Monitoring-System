package com.audit.model;

/**
 * INHERITANCE: extends BaseLog
 * POLYMORPHISM: overrides getSummary(), getCategory(), getLogId()
 */
public class AuthLog extends BaseLog {

    private int    authId;
    private String authType;
    private String authResult;
    private String sourceIp;

    public AuthLog() {}

    public AuthLog(int userId, String authType, String authResult, String sourceIp) {
        this.userId     = userId;
        this.authType   = authType;
        this.authResult = authResult;
        this.sourceIp   = sourceIp;
    }

    public int    getAuthId()             { return authId; }
    public void   setAuthId(int id)       { this.authId = id; }
    public String getAuthType()           { return authType; }
    public void   setAuthType(String t)   { this.authType = t; }
    public String getAuthResult()         { return authResult; }
    public void   setAuthResult(String r) { this.authResult = r; }
    public String getSourceIp()           { return sourceIp; }
    public void   setSourceIp(String ip)  { this.sourceIp = ip; }

    @Override public int    getLogId()    { return authId; }
    @Override public String getCategory(){ return "AUTHENTICATION"; }

    @Override
    public String getSummary() {
        return String.format("[AUTH] User:%d | %s | %s | IP:%s | %s",
                userId, authType, authResult, sourceIp, getFormattedTimestamp());
    }

    @Override
    public String toString() {
        return String.format("%-5d | %-6d | %-7s | %-8s | %-15s | %s",
                authId, userId, authType, authResult, sourceIp, getFormattedTimestamp());
    }
}
