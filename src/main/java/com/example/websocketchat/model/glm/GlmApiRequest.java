package com.example.websocketchat.model.glm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlmApiRequest {
    private String model;
    private List<MessagePayload> messages;
    // 可添加 stream, temperature, top_p 等参数

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePayload {
        private String role;
        private String content;
    }
}