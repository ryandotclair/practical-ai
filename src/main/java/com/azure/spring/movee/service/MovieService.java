package com.azure.spring.movee.service;

import com.azure.ai.openai.models.FunctionCall;
import com.azure.core.util.BinaryData;
import com.azure.spring.movee.address.AddressConverter;
import com.azure.spring.movee.model.MoviesLocation;
import com.google.gson.Gson;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    @Value("classpath:/prompts/movie-prompt.st")
    private Resource moviePrompt;

    private final WebClient webClient;

    @Autowired
    private SimpleVectorStore simpleVectorStore;


    public MovieService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.themoviedb.org").build();
    }

    public Prompt getMoviePrompt() {
        PromptTemplate promptTemplate = new PromptTemplate(moviePrompt);
        return promptTemplate.create();
    }

    public Object executeMovieListFunction(FunctionCall functionCall, List<String> logs) {
        MoviesLocation movieLocation = BinaryData.fromString(functionCall.getArguments())
                .toObject(MoviesLocation.class);
        AddressConverter addressConverter = new AddressConverter();
        String shortName = addressConverter.getCoOrdinates(movieLocation.getRegion());
        String weekBeforeDate = getWeekBeforeDate(movieLocation);
        logs.add("getMovieList parameters: ***"+movieLocation.toString()+"***");
        return webClient.get()
                .uri("/3/discover/movie?with_origin_country="+shortName+"&region="+shortName+"&include_adult=true&include_video=false&language=en-US&page=1&sort_by=popularity.desc&with_release_type=2|3&primary_release_date.gte="+weekBeforeDate+"&primary_release_date.lte="+movieLocation.getDate())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
               .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiYTk1OGZhZDAyM2Y0YzU2YWQ1ODliZjZmYTUzNDc4YSIsInN1YiI6IjY1NjZiN2IyODlkOTdmMDBmZTdjN2Q1NCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.MzwQ2Z0EOv-nIrCQeUBV_d4kl-lo8HCDNbIDEcBKisY")
                .retrieve()
               .bodyToFlux(LinkedHashMap.class)
               .map(t -> {
                   Gson gson = new Gson();
                   if(t.get("results") != null) {
                       return gson.toJson(t.get("results"));
                   }
                   return "";
               })
                .blockFirst();
    }


    public Object executeMovieSearchRecommendations(String question, List<String> logs) {
        SearchRequest query = SearchRequest.query(question);
       // query.withSimilarityThreshold(0.8);
        logs.add("Using the Threshold of ***" + 0.0 + "*** for the "+ "***cosine similarity***");
        List<Document> similarDocuments = simpleVectorStore.similaritySearch(query);
        logs.add("Found ***" + similarDocuments.size() + "*** relevant documents to the user search query");
        Gson gson = new Gson();
        return (!similarDocuments.isEmpty())? gson.toJson(similarDocuments.stream().map(t -> t.getContent()).collect(Collectors.toList())) : "Found 0 relevant documents.";
    }

    private String getWeekBeforeDate(MoviesLocation movieLocation) {
        DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String weekBeforeDate;
        if (movieLocation.getDate() == null) {
            movieLocation.setDate(formatter.format(new Date()));
            LocalDate localDate = LocalDate.now().minusWeeks(1);//
            weekBeforeDate = localDate.format(dateformatter);
        } else {
            LocalDate movieLocationDate = LocalDate.parse(movieLocation.getDate());
            LocalDate subtractedDate = movieLocationDate.minusWeeks(1);
            weekBeforeDate = subtractedDate.format(dateformatter);
        }
        return weekBeforeDate;
    }

}
