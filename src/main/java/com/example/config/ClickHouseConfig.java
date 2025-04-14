package com.example.config;

public class ClickHouseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String token;

    public ClickHouseConfig(String host, int port, String database, String user, String token) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.token = token;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}