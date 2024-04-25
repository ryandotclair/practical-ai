package com.azure.spring.movee.adapter;

import com.azure.ai.openai.models.*;
import com.azure.spring.movee.model.ChatCompletionMessage;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.ai.chat.messages.Message;


public class ChatMessageAdapter {
    public static Message fromSpring(ChatCompletionMessage message) {
        return switch (message.getRole()) {
            case USER -> new ChatMessage("USER", message.getContent());
            case SYSTEM -> new ChatMessage("SYSTEM", message.getContent());
            case ASSISTANT -> new ChatMessage("ASSISTANT", message.getContent());
            case FUNCTION -> new ChatMessage("FUNCTION", message.getContent());
            default -> throw new IllegalArgumentException("Unknown message type " + message.getRole());
        };
    }

    public static ChatRequestMessage from(ChatCompletionMessage message) {
        return switch (message.getRole()) {
            case USER -> new ChatRequestUserMessage(message.getContent());
            case SYSTEM -> new ChatRequestSystemMessage(message.getContent());
            case ASSISTANT -> new ChatRequestAssistantMessage(message.getContent());
            case FUNCTION -> new ChatRequestFunctionMessage("function", message.getContent());
            default -> throw new IllegalArgumentException("Unknown message type " + message.getRole());
        };
    }

}
