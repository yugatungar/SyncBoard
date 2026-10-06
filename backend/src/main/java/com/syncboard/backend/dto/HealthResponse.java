package com.syncboard.backend.dto;

public class HealthResponse {

    private String application;
    private String database;
    private String redis;

    public HealthResponse() {
    }

    public HealthResponse(String application, String database, String redis) {
        this.application = application;
        this.database = database;
        this.redis = redis;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRedis() {
        return redis;
    }

    public void setRedis(String redis) {
        this.redis = redis;
    }
}
