package com.audit.model;

/**
 * INHERITANCE: extends BaseLog
 * POLYMORPHISM: overrides getSummary(), getCategory(), getLogId()
 */
public class InputValidationLog extends BaseLog {

    private int    validationLogId;
    private String inputField;
    private String failureReason;
    private String sourceIp;

    public InputValidationLog() {}

    public InputValidationLog(int userId, String inputField, String failureReason, String sourceIp) {
        this.userId        = userId;
        this.inputField    = inputField;
        this.failureReason = failureReason;
        this.sourceIp      = sourceIp;
    }

    public int    getValidationLogId()         { return validationLogId; }
    public void   setValidationLogId(int id)   { this.validationLogId = id; }
    public String getInputField()              { return inputField; }
    public void   setInputField(String f)      { this.inputField = f; }
    public String getFailureReason()           { return failureReason; }
    public void   setFailureReason(String r)   { this.failureReason = r; }
    public String getSourceIp()                { return sourceIp; }
    public void   setSourceIp(String ip)       { this.sourceIp = ip; }

    @Override public int    getLogId()    { return validationLogId; }
    @Override public String getCategory(){ return "INPUT_VALIDATION"; }

    @Override
    public String getSummary() {
        return String.format("[VALIDATION] User:%d | Field:%s | %s | IP:%s | %s",
                userId, inputField, failureReason, sourceIp, getFormattedTimestamp());
    }

    @Override
    public String toString() {
        return String.format("%-5d | UID:%-4d | %-15s | %-30s | %-15s | %s",
                validationLogId, userId, inputField, failureReason, sourceIp, getFormattedTimestamp());
    }
}
