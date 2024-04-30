package com.azure.spring.movee.embeddings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class LoadEmbeddings  implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private StoreEmbeddings storeEmbeddings;
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            storeEmbeddings.load();
        } catch (IOException e) {
            log.error("Exception while loading embeddings: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
