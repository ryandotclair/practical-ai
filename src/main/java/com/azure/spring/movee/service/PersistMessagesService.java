package com.azure.spring.movee.service;

import com.azure.spring.movee.model.ChatCompletionMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PersistMessagesService {

    @Value("classpath:/prompts/system-qa.st")
    private Resource systemPrompt;

    private final RedisTemplate redisTemplate;

    public PersistMessagesService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void reset(String userIpAddress) {
        redisTemplate.delete(userIpAddress);
        redisTemplate.delete("logs-"+userIpAddress);
        redisTemplate.delete(userIpAddress+"-prompt");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Prompt prompt = systemPromptTemplate.create(Map.of("date", dtf.format(LocalDateTime.now())));
        persistPrompts(prompt.getContents(), userIpAddress);
    }

    public void persistMessages(List<ChatCompletionMessage> history, String userIpAddress) {
        if (userIpAddress.isEmpty() || userIpAddress.isBlank()) return;

        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        redisDetails.set(userIpAddress, gson.toJson(history));
    }

    public void persistPrompts(String prompt, String userIpAddress) {
        if (userIpAddress.isEmpty() || userIpAddress.isBlank()) return;

        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        redisDetails.set(userIpAddress+"-prompt", prompt);
    }

    public String getPrompts(String userIpAddress) {
        if (userIpAddress.isEmpty() || userIpAddress.isBlank()) return null;

        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        return redisDetails.get(userIpAddress+"-prompt");
    }

    public void persistLogs(List<String> logs, String userIpAddress) {
        if (userIpAddress.isEmpty() || userIpAddress.isBlank()) return;

        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        redisDetails.set("logs-"+userIpAddress, gson.toJson(logs));
    }

    public List<String> getLogs(String userIpAddress) {
        if (userIpAddress.isEmpty() || userIpAddress.isBlank()) return List.of("");

        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        if (redisDetails.get("logs-"+userIpAddress) != null) {
            return gson.fromJson(redisDetails.get("logs-"+userIpAddress), listType);
        }
        return null;
    }

    public List<ChatCompletionMessage> getMessages(String userIpAddress) {
        ValueOperations<String, String> redisDetails = redisTemplate.opsForValue();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ChatCompletionMessage>>() {}.getType();
        if(userIpAddress != null && redisDetails.get(userIpAddress) != null) {
            return gson.fromJson(redisDetails.get(userIpAddress), listType);
        }
        return null;
    }
}
