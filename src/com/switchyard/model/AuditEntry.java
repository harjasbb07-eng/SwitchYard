package com.switchyard.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AuditEntry {
    private final long timestamp;
    private final String stateName;
    private final String message;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    public AuditEntry(long timestamp, String stateName, String message) {
        this.timestamp = timestamp;
        this.stateName = stateName;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStateName() {
        return stateName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s", FORMATTER.format(Instant.ofEpochMilli(timestamp)), stateName, message);
    }
}