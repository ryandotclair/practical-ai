package com.azure.spring.movee.vectorstore;

import com.azure.spring.movee.reader.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@Slf4j
public class DocumentIndexPlanner {


    private final SimpleVectorStore vectorStore;

    public DocumentIndexPlanner(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void buildFromFolder(Resource folder) throws IOException {
        if (folder == null) {
            throw new IllegalArgumentException("folderPath shouldn't be empty.");
        }

        JsonReader jsonReader = new JsonReader(folder, "genres", "original_title", "overview", "vote_average");
        List<Document> documents = jsonReader.get();
        vectorStore.add(documents);
        log.info("All documents are loaded to the vector store.");
    }
}
