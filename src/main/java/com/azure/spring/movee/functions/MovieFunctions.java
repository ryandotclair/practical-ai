package com.azure.spring.movee.functions;

import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinition;
import com.azure.ai.openai.models.ChatCompletionsToolDefinition;
import com.azure.ai.openai.models.FunctionCall;
import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.util.BinaryData;
import com.azure.spring.movee.model.ChatFunctionDetails;
import com.azure.spring.movee.model.MovieParameters;
import com.azure.spring.movee.model.MovieRecommendationParameters;
import com.azure.spring.movee.service.MovieService;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class MovieFunctions {

    private final MovieService movieService;
    public MovieFunctions(MovieService movieService) {
        this.movieService = movieService;
    }

    public List<ChatCompletionsToolDefinition> openAIFunctions() {
        return Arrays.asList(
                new ChatCompletionsFunctionToolDefinition(getMovieListFunctionDefinition()),
                new ChatCompletionsFunctionToolDefinition(getSimilarMoviesFunctionDefinition())
        );
    }

    private FunctionDefinition getMovieListFunctionDefinition() {
        FunctionDefinition functionDefinition = new FunctionDefinition("getMovieList");
        functionDefinition.setDescription("This function returns a json payload of all currently running movies. Unless specified by the user, you will return all movies in this function. As part of the payload the key called {release_date} is formatted as year-month-day. {release_date} is date in which the movie was created and not when it is running in theaters.");
        MovieParameters parameters = new MovieParameters();
        functionDefinition.setParameters(BinaryData.fromObject(parameters));
        return functionDefinition;
    }

    private FunctionDefinition getSimilarMoviesFunctionDefinition() {
        FunctionDefinition functionDefinition = new FunctionDefinition("getSimilarMovies");
        functionDefinition.setDescription("This function returns similar movies.");
        MovieRecommendationParameters parameters = new MovieRecommendationParameters();
        functionDefinition.setParameters(BinaryData.fromObject(parameters));
        return functionDefinition;
    }

    public Object executeFunctionCall(ChatFunctionDetails chatFunctionDetails, String question, List<String> logs) {
        FunctionCall functionCall = new FunctionCall(chatFunctionDetails.getFunctionName(), chatFunctionDetails.getFunctionArguments().toString());
        if(functionCall.getName().equals("getMovieList")) {
            return movieService.executeMovieListFunction(functionCall, logs);
        }
            return movieService.executeMovieSearchRecommendations(question, logs);
        // This message contains the information that will allow the LLM to resume the text generation
    }

}
