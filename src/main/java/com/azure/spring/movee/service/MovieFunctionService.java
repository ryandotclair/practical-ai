package com.azure.spring.movee.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.util.IterableStream;
import com.azure.spring.movee.functions.MovieFunction;
import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.model.ChatFunctionDetails;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MovieFunctionService {

    private final MovieFunction movieList;
    private final MovieFunction movieRecommendation;

    private final OpenAIClient openAIClient;

    @Value("${spring.ai.azure.openai.chat.options.model}")
    private String deploymentOrModelId;

    @Value("classpath:/prompts/format-embeddings-prompt.st")
    private Resource formatPrompt;

    @Value("classpath:/prompts/one-shot-prompt.st")
    private Resource oneShotPromptResource;

    @Autowired
    public MovieFunctionService(@Qualifier("movieList") MovieFunction movieList,
                                @Qualifier("movieRecommendations") MovieFunction movieRecommendation,
                                OpenAIClient openAIClient) {
        this.movieList = movieList;
        this.movieRecommendation = movieRecommendation;
        this.openAIClient = openAIClient;
    }

    public List<ChatCompletionsToolDefinition> openAIFunctions() {
        return Arrays.asList(
                new ChatCompletionsFunctionToolDefinition(movieList.getFunctionDefinition()),
                new ChatCompletionsFunctionToolDefinition(movieRecommendation.getFunctionDefinition())
        );
    }


    public Stream<String> executeFunctionTools(String userIp, ChatFunctionDetails functionDetails, List<String> logs,
                                                List<ChatRequestMessage> followUpMessages, List<ChatCompletionMessage> additionalMessages,
                                                String question, List<String> outputData) {
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
            question = augmentMovieRecommendationPrompts(followUpMessages, logs, additionalMessages);
        }
        functionCallResult = executeFunction(functionDetails, question, logs);
        logs.add("The tools call result: \n\n ```" + functionCallResult.toString() + "```");
        ChatRequestToolMessage toolRequestMessage = new ChatRequestToolMessage(functionCallResult.toString(), functionDetails.getToolID());
        followUpMessages.add(assistantRequestMessage);
        followUpMessages.add(toolRequestMessage);

        //Continue to maintain chatCompletionMessages for logging purpose
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

    public Object executeFunction(ChatFunctionDetails chatFunctionDetails, String question, List<String> logs) {
        FunctionCall functionCall = new FunctionCall(chatFunctionDetails.getFunctionName(), chatFunctionDetails.getFunctionArguments().toString());
        if(functionCall.getName().equals("getMovieList")) {
            return movieList.execute(functionCall, null, logs);
        }
        return movieRecommendation.execute(null, question, logs);
    }

    private String augmentMovieRecommendationPrompts(List<ChatRequestMessage> followUpMessages, List<String> logs, List<ChatCompletionMessage> additionalMessages) {
        PromptTemplate oneShotPromptTemplate = new PromptTemplate(oneShotPromptResource);
        Prompt oneShotPrompt = oneShotPromptTemplate.create();
        ChatRequestUserMessage chatRequestEnhancedUserMessage = new ChatRequestUserMessage(oneShotPrompt.getContents());
        List<ChatRequestMessage> copyConversationMessages = new ArrayList<>(followUpMessages);
        copyConversationMessages.add(chatRequestEnhancedUserMessage);
        logs.add("Creating one-shot Prompt: ***" + oneShotPrompt.getContents() + "***");
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
        String formattedPrompt = "Ensure the returned list of movies is in a numbered list format, with both movie title and rating in the same line, and in preceding sub-bullets include genre and overview for that movie. NEVER truncate the results and list every movie. Example output you should use:\n 1. ***{original_title}*** - **Rating** {avg_vote} \n  * **Genres**: {genres} \n * **Overview**: {overview}\n";
        ChatRequestUserMessage formattedUserMessage = new ChatRequestUserMessage(formattedPrompt);
        followUpMessages.add(formattedUserMessage);
        logs.add("Added formatting to the conversation: **"+formattedPrompt);
        return prompt;
    }

    private void augmentMovieListPrompts(List<ChatRequestMessage> followUpMessages, List<String> logs, List<ChatCompletionMessage> additionalMessages) {
        String treeOfThoughtPrompt = "Imagine three brilliant, logical experts collaboratively answering the above question. Each one verbosely explains their thought process in real-time, considering the prior explanations of others and openly acknowledging mistakes. At each step, whenever possible, each expert refines and builds upon the thoughts of others, acknowledging their contributions. They continue until there is a definitive answer to the question. The final agreed upon answer should be given in a markdown numbered list format, in the format of ***{original_title}*** - ***Release Date***: {release_date} - ***Rating***:{avg_vote}. As a sub-bullet SUMMARIZE {overview} in ONE LINE under **Overview**. IGNORE the {poster_path}. List every movie in the markdown format.";
        ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(treeOfThoughtPrompt);
        followUpMessages.add(chatRequestUserMessage);
        logs.add("Prompt Engineering strategy used: ***Tree of Thought***");
        logs.add("Augmented Prompt: ***" + treeOfThoughtPrompt + "***");
        // Continue to maintain chatCompletionMessages
        ChatCompletionMessage userMessage = new ChatCompletionMessage(ChatCompletionMessage.Role.USER, treeOfThoughtPrompt);
        additionalMessages.add(userMessage);
    }

}
