package com.azure.spring.movee.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.util.IterableStream;
import com.azure.spring.movee.functions.MovieFunction;
import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.model.ChatFunctionDetails;
import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MovieFunctionService {

    private final MovieFunction movieList;
    private final MovieFunction movieRecommendation;

    @Autowired
    public MovieFunctionService(@Qualifier("movieList") MovieFunction movieList, @Qualifier("movieRecommendations") MovieFunction movieRecommendation) {
        this.movieList = movieList;
        this.movieRecommendation = movieRecommendation;
    }

    public List<ChatCompletionsToolDefinition> openAIFunctions() {
        return Arrays.asList(
                new ChatCompletionsFunctionToolDefinition(movieList.getFunctionDefinition()),
                new ChatCompletionsFunctionToolDefinition(movieRecommendation.getFunctionDefinition())
        );
    }
    public Object executeFunctionCall(ChatFunctionDetails chatFunctionDetails, String question, List<String> logs) {
        FunctionCall functionCall = new FunctionCall(chatFunctionDetails.getFunctionName(), chatFunctionDetails.getFunctionArguments().toString());
        if(functionCall.getName().equals("getMovieList")) {
            return movieList.execute(functionCall, null, logs);
        }
        return movieRecommendation.execute(null, question, logs);
    }

    public Stream<String> executeFunctionTools(String userIp, ChatFunctionDetails functionDetails, List<String> logs,
                                                List<ChatRequestMessage> followUpMessages, List<ChatCompletionMessage> additionalMessages,
                                                String question, List<String> outputData,
                                                OpenAIClient openAIClient, String deploymentOrModelId) {
        Object functionCallResult;
        FunctionCall functionCall = new FunctionCall(functionDetails.getFunctionName(), functionDetails.getFunctionArguments().toString());
        ChatCompletionsFunctionToolCall functionToolCall = new ChatCompletionsFunctionToolCall(functionDetails.getToolID(), functionCall);
        ChatRequestAssistantMessage assistantRequestMessage = new ChatRequestAssistantMessage("");
        assistantRequestMessage.setToolCalls(List.of(functionToolCall));
        logs.add("The model needs more data and is running function: ***" + functionDetails.getFunctionName() + "***");
        if (functionCall.getName().equals("getMovieList")) {
            augmentMovieListPrompts(followUpMessages, logs, additionalMessages);
        }
        if (functionCall.getName().equals("getSimilarMovies")) {
            question = augmentMovieRecommendationPrompts(followUpMessages, logs, additionalMessages, openAIClient, deploymentOrModelId);
        }
        functionCallResult = executeFunctionCall(functionDetails, question, logs);
        logs.add("The tools call result: \n\n ```" + functionCallResult.toString() + "```");
        ChatRequestToolMessage toolRequestMessage = new ChatRequestToolMessage(functionCallResult.toString(), functionDetails.getToolID());
        if (functionCall.getName().equals("getSimilarMovies")) {
            String formattingPrompt = "Ensure the returned list of movies is in a numbered list format, with both movie title and rating in the same line, and in preceding sub-bullets include genre and overview for that movie. NEVER truncate the results and list every movie. Example output you should use:\n 1. ***{original_title}*** - **Rating** {avg_vote} \n    * **Genres**: {genres} \n    * **Overview**: {overview}\n";
            ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(formattingPrompt);
            followUpMessages.add(chatRequestUserMessage);
            logs.add("Added formatting to the conversation: **Ensure the returned movies is in a markdown numbered list, with **{movie title}** - Rating {rating}, a sub-bullet for Genres: {genres} and a sub-bullet for ‘Overview: {overview}**\n");
        }
        followUpMessages.add(assistantRequestMessage);
        followUpMessages.add(toolRequestMessage);

        //Continue to maintain chatCompletionMessages
        ChatCompletionMessage toolCompletionMessage = new ChatCompletionMessage(ChatCompletionMessage.Role.TOOL, functionCallResult.toString(), functionDetails.getToolID());
        additionalMessages.add(toolCompletionMessage);
        IterableStream<ChatCompletions> followUpChatCompletionsStream = openAIClient.getChatCompletionsStream(
                deploymentOrModelId, new ChatCompletionsOptions(followUpMessages));
        return followUpChatCompletionsStream
                        .stream()
                        .skip(1)
                        .map(chatCompletions -> {
                            ChatChoice choice = chatCompletions.getChoices().get(0);
                            if (choice.getDelta().getContent() != null) {
                                outputData.add(choice.getDelta().getContent());
                                if (choice.getDelta().getContent().contains("\n\n") && choice.getFinishReason() != CompletionsFinishReason.STOPPED) {
                                    return choice.getDelta().getContent().replace("\n\n", "\n");
                                }
                                return choice.getDelta().getContent();
                            }
                            return "";
                        });
    }

    private String augmentMovieRecommendationPrompts(List<ChatRequestMessage> followUpMessages, List<String> logs, List<ChatCompletionMessage> additionalMessages,
                                                     OpenAIClient openAIClient, String deploymentOrModelId) {
        String question;
        String enhancedPrompt = "Generate an even better search prompt than my previous prompt. The search prompt will be used against an embedding datastore of combined movie synopsis and genres. Do not include the movie title. Reply back with ONLY the new search prompt. No AI commentary";
        ChatRequestUserMessage chatRequestEnhancedUserMessage = new ChatRequestUserMessage(enhancedPrompt);
        List<ChatRequestMessage> copyConversationMessages = new ArrayList<>(followUpMessages);
        copyConversationMessages.add(chatRequestEnhancedUserMessage);
        //followUpMessages.add(chatRequestEnhancedUserMessage);
        logs.add("Creating one-shot Prompt: ***" + enhancedPrompt + "***");
        IterableStream<ChatCompletions> enhancedPromptCompletionStream = openAIClient.getChatCompletionsStream(
                deploymentOrModelId, new ChatCompletionsOptions(copyConversationMessages));
        List<String> modelPrompt = new ArrayList<>();
        enhancedPromptCompletionStream.stream()
                .skip(1L)
                .forEach(chatCompletions -> {
                    ChatChoice choice = chatCompletions.getChoices().get(0);
                    if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                        modelPrompt.add(choice.getDelta().getContent());
                    }
                });
        String prompt = !modelPrompt.isEmpty() ? String.join("", modelPrompt) : modelPrompt.toString();
        ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(prompt);
        followUpMessages.add(chatRequestUserMessage);
        logs.add("Model Generated Prompt: **" + prompt + "**");
        // Continue to maintain chatCompletionMessages
        ChatCompletionMessage userMessage = new ChatCompletionMessage(ChatCompletionMessage.Role.USER, prompt);
        additionalMessages.add(userMessage);
        logs.add("The Embedding model used to search the user query: ***text-embedding-ada-002***");
        question = prompt;
        return question;
    }

    private static void augmentMovieListPrompts(List<ChatRequestMessage> followUpMessages, List<String> logs, List<ChatCompletionMessage> additionalMessages) {
        String prompt = "Imagine three brilliant, logical experts collaboratively answering the above question. Each one verbosely explains their thought process in real-time, considering the prior explanations of others and openly acknowledging mistakes. At each step, whenever possible, each expert refines and builds upon the thoughts of others, acknowledging their contributions. They continue until there is a definitive answer to the question. The final agreed upon answer should be given in a markdown numbered list format, in the format of ***{original_title}*** - ***Release Date***: {release_date} - ***Rating***:{avg_vote}. As a sub-bullet include ***Overview***: {overview}. IGNORE the {poster_path}. NEVER truncate the results. List every movie in the markdown format.";
        ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(prompt);
        followUpMessages.add(chatRequestUserMessage);
        logs.add("Prompt Engineering strategy used: ***Tree of Thought***");
        logs.add("Augmented Prompt: ***" + prompt + "***");
        // Continue to maintain chatCompletionMessages
        ChatCompletionMessage userMessage = new ChatCompletionMessage(ChatCompletionMessage.Role.USER, prompt);
        additionalMessages.add(userMessage);
    }

}
