package com.azure.spring.movee.functions;

import com.azure.ai.openai.models.FunctionCall;
import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.util.BinaryData;
import com.azure.spring.movee.address.AddressConverter;
import com.azure.spring.movee.model.MovieParameters;
import com.azure.spring.movee.model.MoviesLocation;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@Qualifier("movieList")
public class MovieListFunction implements MovieFunction {

    private final WebClient webClient;

    @Value("${tmdb.api.auth.token}")
    private String apiAuthToken;

    public MovieListFunction(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.themoviedb.org").build();
    }
    public FunctionDefinition getFunctionDefinition() {
        FunctionDefinition functionDefinition = new FunctionDefinition("getMovieList");
        functionDefinition.setDescription("This function returns a json payload of all currently running movies. Unless specified by the user, you will return all movies in this function. As part of the payload the key called {release_date} is formatted as year-month-day. {release_date} is date in which the movie was created and not when it is running in theaters.");
        MovieParameters parameters = new MovieParameters();
        functionDefinition.setParameters(BinaryData.fromObject(parameters));
        return functionDefinition;
    }

    @Override
    public Object execute(FunctionCall functionCall, String question, List<String> logs) {
        MoviesLocation movieLocation = BinaryData.fromString(functionCall.getArguments())
                .toObject(MoviesLocation.class);
        AddressConverter addressConverter = new AddressConverter();
        String shortName = addressConverter.getCoOrdinates(movieLocation.getRegion());
        String weekBeforeDate = getWeekBeforeDate(movieLocation);
        logs.add("getMovieList parameters: ***"+movieLocation.toString()+"***");
        return webClient.get()
                .uri("/3/discover/movie?with_origin_country="+shortName+"&region="+shortName+"&include_adult=true&include_video=false&language=en-US&page=1&sort_by=popularity.desc&with_release_type=2|3&primary_release_date.gte="+weekBeforeDate+"&primary_release_date.lte="+movieLocation.getDate())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer "+ apiAuthToken)
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
