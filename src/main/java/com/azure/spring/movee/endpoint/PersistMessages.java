package com.azure.spring.movee.endpoint;


import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.service.PersistMessagesService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;

import java.util.List;

@Endpoint
@AnonymousAllowed
public class PersistMessages {

    private final PersistMessagesService persistMessagesService;

    public PersistMessages(PersistMessagesService persistMessagesService) {
        this.persistMessagesService = persistMessagesService;
    }

    public void persistMessages(List<ChatCompletionMessage> history, String userIpAddress) {
        persistMessagesService.persistMessages(history, userIpAddress);
    }

    public List<ChatCompletionMessage> retrieveMessages(String userIpAddress) {
       return persistMessagesService.getMessages(userIpAddress);
    }

    public List<String> getLogs(String userIpAddress) {
        return persistMessagesService.getLogs(userIpAddress);
    }

    public String getPrompts(String userIpAddress) {
        return persistMessagesService.getPrompts(userIpAddress);
    }

    public void reset(String userIpAddress) {
        persistMessagesService.reset(userIpAddress);
    }
}
