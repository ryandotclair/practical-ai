package com.azure.spring.movee.endpoint;

import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.service.AzureChatService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Endpoint
@AnonymousAllowed
@RestController
public class AzureChatController {

    private final AzureChatService chatService;

    public AzureChatController(AzureChatService chatService) {
        this.chatService = chatService;
    }

    public Flux<String> getChats(List<ChatCompletionMessage> history, String userIp) {
        return chatService.getCompletionStream(history, userIp);
    }

    @GetMapping(path = "/chats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getBrowserChat(@RequestParam String message) {
        return chatService.getCompletionStream(Arrays.asList(new ChatCompletionMessage(ChatCompletionMessage.Role.USER, "Hello"),
                new ChatCompletionMessage(ChatCompletionMessage.Role.ASSISTANT, "Fantastic, thanks! How can I assist you today?"),
                new ChatCompletionMessage(ChatCompletionMessage.Role.USER, message)), "10.15.200.192");
    }


    public void savePrompt(String newPrompt, String userIp) {
        chatService.savePrompt(newPrompt, userIp);
    }

}
