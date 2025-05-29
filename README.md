use `mvn clean package` to build.

# WebSocket Chat Application
## Access here for more info↓
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/godsboy404/websocketchat)

A real-time chat platform built with Spring Boot, JavaScript, and WebSocket/STOMP, featuring public/private messaging and AI bot integration via the GLM API.

## Relevant Source Files

| File/Path                       | Description                                                    |
| ------------------------------- | -------------------------------------------------------------- |
| `WebsocketchatApplication.java` | Main Spring Boot application class: Entry point and scheduling |
| `WebSocketConfig.java`          | Configures WebSocket endpoints and STOMP                       |
| `ChatController.java`           | Handles message routing and AI bot communication               |
| `WebSocketEventListener.java`   | Handles user connection/disconnection events                   |
| `SecurityConfig.java`           | Secures WebSocket endpoints: Auth & WebSocket security         |
| `pom.xml`                       | Project dependencies and build configuration                   |
| `README.md`                     | Project documentation                                          |

## Message Flow

1. User connects via WebSocket.
2. A JOIN event is broadcast.
3. Messages are sent:
    - To /app/message for public
    - To /app/private-message for private
4. Server responds:
    - To /topic/messages for public
    - To /user/{username}/private for private
5. Messages may trigger a call to the GLM API for bot replies.
6. Bot replies are broadcasted similarly.

## System Architecture

### Layers

- Client Layer: JavaScript Web client, interacts via STOMP over WebSocket.
- Server Layer: Spring Boot handles real-time messaging, user management, and security.
- External Services: GLM AI Service for generating bot replies.

### Components

- Web Browser Client: JavaScript UI with WebSocket connection
- WebSocketConfig: Defines broker relay and application destination prefixes
- ChatController: Entry point for all messages
- GLM API Integration: Uses Spring WebClient to query AI models
- WebSocketEventListener: Monitors connect/disconnect events
- SecurityConfig: Manages WebSocket connection authentication

## Key Features

| Feature                | Description                               |
| ---------------------- | ----------------------------------------- |
| Real-time Messaging    | Bi-directional messaging using WebSocket  |
| Public Chat            | Broadcast to all connected users          |
| Private Chat           | One-to-one encrypted communication        |
| Online Presence        | Tracks user join/leave with activity logs |
| AI Bot Integration     | Responds to user input using GLM API      |
| Scheduled Bot Messages | Periodic system or bot-generated messages |

## Tech Stack

Java 11, Spring Boot 2.7.x, Spring WebSocket / STOMP, Spring Security, Spring WebClient, JavaScript (Vanilla/DOM), Maven

## Setup & Deployment

### Prerequisites

Java 11+, Maven 3+, GLM API Key (environment or config)

### Steps

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Access
http://localhost:8080
```

## License

This project uses GNU GPL 3.0 License.
