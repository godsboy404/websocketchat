package com.example.websocketchat.model;

public class PrivateChatMessage {
    private String sender;
    private String recipient;
    private String message;
    private String timestamp;

    public PrivateChatMessage() {
    }

    public PrivateChatMessage(String sender, String recipient, String message, String timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
