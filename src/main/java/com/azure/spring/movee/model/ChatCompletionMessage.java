package com.azure.spring.movee.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionMessage {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),

        FUNCTION("function"),

        TOOL("tool");

        private final String role;

        Role(String role) {
            this.role = role;
        }

        @Override
        @JsonValue
        public String toString() {
            return role;
        }

        @JsonCreator
        public static Role fromString(String value) {
            return Role.valueOf(value.toUpperCase());
        }
    }

    private Role role;
    private String content;

    private String tool_call_id;

    public ChatCompletionMessage() {
    }

    public ChatCompletionMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatCompletionMessage(Role role, String content, String tool_call_id) {
        this.role = role;
        this.content = content;
        this.tool_call_id = tool_call_id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public String getToolCallId() {
        return tool_call_id;
    }

    public void setContent(String content) {
        this.content = content;
    }


    @Override
    public String toString() {
        return "ChatCompletionMessage{" +
                "role=" + role +
                "tool_call_id=" + tool_call_id +
                ", content='" + content + '\'' +
                '}';
    }
}
