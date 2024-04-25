package com.azure.spring.movee.functions;

import com.azure.ai.openai.models.FunctionCall;
import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.util.BinaryData;
import com.azure.spring.movee.model.MovieParameters;
import com.azure.spring.movee.model.MovieRecommendationParameters;
import com.google.gson.Gson;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Qualifier("movieRecommendations")
public class MovieRecommendationsFunction implements MovieFunction {

    @Autowired
    private SimpleVectorStore simpleVectorStore;
    public FunctionDefinition getFunctionDefinition() {
        FunctionDefinition functionDefinition = new FunctionDefinition("getSimilarMovies");
        functionDefinition.setDescription("This function recommends 4 similar movies based on the characteristics of a movie. For best results, use the {overview} data from getMovieList function to create a strong movie description, and include any overarching themes or tones.");
        MovieRecommendationParameters parameters = new MovieRecommendationParameters();
        functionDefinition.setParameters(BinaryData.fromObject(parameters));
        return functionDefinition;
    }

    public Object execute(FunctionCall functionCall, String question, List<String> logs) {
        logs.add("Using the Threshold of ***" + 0.0 + "*** for the "+ "***cosine similarity***");
        List<Document> similarDocuments = simpleVectorStore.similaritySearch(question);
        logs.add("Found ***" + similarDocuments.size() + "*** relevant documents to the user search query");
        Gson gson = new Gson();
        return (!similarDocuments.isEmpty())? gson.toJson(similarDocuments.stream().map(Document::getContent).collect(Collectors.toList())) : "Found 0 relevant documents.";
    }

}
