package com.example.websocketchat.model;

public class UserJoinEvent {
    private String username;
    private String type; // "JOIN" or "LEAVE"
    private String timestamp;

    public UserJoinEvent() {
    }

    public UserJoinEvent(String username, String type, String timestamp) {
        this.username = username;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
