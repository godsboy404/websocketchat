package com.example.websocketchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserJoinEvent {
    private String username;
    private String type; // "JOIN", "LEAVE", "PING"
    private Instant timestamp; // 改为 Instant 类型
}