use `mvn clean package` to build.

# WebSocket 聊天应用 [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/godsboy404/websocketchat)

这是一个基于 WebSocket 的聊天应用，使用 Spring Boot 和 JavaScript 构建。

## 功能

- 使用 WebSocket 实现实时消息传递
- 公共和私人聊天功能
- 机器人回复与 AI 集成
- 用户加入/离开通知
- 定期机器人消息
- 在线用户列表管理

## 技术栈

- Java
- Spring Boot
- JavaScript
- Maven
- WebSocket
- WebClient

## 设置

1. 克隆仓库：
    ```sh
    git clone https://github.com/godsboy404/websocketchat.git
    cd websocketchat
    ```

2. 更新 `ChatController.java` 中的 API 密钥：
    ```java
    private static final String API_KEY = "your-api-key-here";
    ```

3. 使用 Maven 构建项目：
    ```sh
    mvn clean install
    ```

4. 运行应用：
    ```sh
    mvn spring-boot:run
    ```

5. 打开浏览器并导航到 `http://localhost:8080` 开始聊天。

## 许可证

此项目使用 MIT 许可证。
