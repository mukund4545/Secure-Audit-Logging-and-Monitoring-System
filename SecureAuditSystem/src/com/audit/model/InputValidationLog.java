package com.audit.model;

import java.time.LocalDateTime;

public class InputValidationLog {
    private int    validationLogId;
    private int    userId;          // nullable → stored as 0 if unknown
    private String inputField;
    private String failureReason;
    private String sourceIp;
    private LocalDateTime timestamp;

    public InputValidationLog() {}

    public InputValidationLog(int userId, String inputField, String failureReason, String sourceIp) {
        this.userId        = userId;
        this.inputField    = inputField;
        this.failureReason = failureReason;
        this.sourceIp      = sourceIp;
    }

    public int    getValidationLogId()                { return validationLogId; }
    public void   setValidationLogId(int id)          { this.validationLogId = id; }
    public int    getUserId()                         { return userId; }
    public void   setUserId(int id)                   { this.userId = id; }
    public String getInputField()                     { return inputField; }
    public void   setInputField(String f)             { this.inputField = f; }
    public String getFailureReason()                  { return failureReason; }
    public void   setFailureReason(String r)          { this.failureReason = r; }
    public String getSourceIp()                       { return sourceIp; }
    public void   setSourceIp(String ip)              { this.sourceIp = ip; }
    public LocalDateTime getTimestamp()               { return timestamp; }
    public void   setTimestamp(LocalDateTime ts)      { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("%-5d | UID:%-4d | %-15s | %-30s | %-15s | %s",
                validationLogId, userId, inputField, failureReason, sourceIp, timestamp);
    }
}
