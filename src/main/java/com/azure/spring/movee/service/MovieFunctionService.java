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

    @Value("classpath:/prompts/tree-of-thought-prompt.st")
    private Resource treeOfThoughtPromptResource;

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
        String question;
        PromptTemplate oneShotPromptTemplate = new PromptTemplate(oneShotPromptResource);
        Prompt oneShotPrompt = oneShotPromptTemplate.create();
        ChatRequestUserMessage chatRequestEnhancedUserMessage = new ChatRequestUserMessage(oneShotPrompt.getContents());
        List<ChatRequestMessage> copyConversationMessages = new ArrayList<>(followUpMessages);
        copyConversationMessages.add(chatRequestEnhancedUserMessage);
        //followUpMessages.add(chatRequestEnhancedUserMessage);
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
        PromptTemplate promptTemplate = new PromptTemplate(formatPrompt);
        Prompt formattedPrompt = promptTemplate.create();
        ChatRequestUserMessage formattedUserMessage = new ChatRequestUserMessage(formattedPrompt.getContents());
        followUpMessages.add(formattedUserMessage);
        logs.add("Added formatting to the conversation: **"+formattedPrompt.getContents());
        return prompt;
    }

    private void augmentMovieListPrompts(List<ChatRequestMessage> followUpMessages, List<String> logs, List<ChatCompletionMessage> additionalMessages) {
        PromptTemplate treeOfThoughtPromptTemplate = new PromptTemplate(treeOfThoughtPromptResource);
        Prompt treeOfThoughtPrompt = treeOfThoughtPromptTemplate.create();
        ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(treeOfThoughtPrompt.getContents());
        followUpMessages.add(chatRequestUserMessage);
        logs.add("Prompt Engineering strategy used: ***Tree of Thought***");
        logs.add("Augmented Prompt: ***" + treeOfThoughtPrompt.getContents() + "***");
        // Continue to maintain chatCompletionMessages
        ChatCompletionMessage userMessage = new ChatCompletionMessage(ChatCompletionMessage.Role.USER, treeOfThoughtPrompt.getContents());
        additionalMessages.add(userMessage);
    }

}
