package com.azure.spring.movee.embeddings;

import com.azure.spring.movee.vectorstore.DocumentIndexPlanner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Endpoint(id="store-embeddings")
@RequiredArgsConstructor
public class StoreEmbeddings {

    private final DocumentIndexPlanner indexPlanner;

    @Value("classpath:/data/5000_movies.json")
    private Resource vectorJsonFile;

    @WriteOperation
    public void load() throws IOException {
        indexPlanner.buildFromFolder(vectorJsonFile);
    }
}


