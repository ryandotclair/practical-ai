package com.azure.spring.movee.vectorstore;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;

public class PromptTemplate {

    private static final String template = """
            Context information is below.
            ---------------------
            %s
            ---------------------
            """;

    public static String formatWithContext(List<Document> context, String question) {
        String documents = context.stream().map(entry -> entry.getContent()).collect(Collectors.joining("\n"));
        return String.format("Answer the question: '%s' based on the provided context: %s ", question, documents);
    }
}
