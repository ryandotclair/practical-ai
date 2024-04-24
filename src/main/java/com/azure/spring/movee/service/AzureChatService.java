package com.azure.spring.movee.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.util.IterableStream;
import com.azure.spring.movee.adapter.ChatMessageAdapter;
import com.azure.spring.movee.functions.MovieFunctions;
import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.model.ChatFunctionDetails;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.azure.spring.movee.model.ChatCompletionMessage.*;

@Service
@Slf4j
public class AzureChatService {
    private final Encoding tokenizer;
    private final int MAX_TOKENS = 4096;
    private final int MAX_RESPONSE_TOKENS = 1024;
    private final int MAX_CONTEXT_TOKENS = 1536;
    private final PersistMessagesService persistMessagesService;

    @Value("classpath:/prompts/system-qa.st")
    private Resource systemPrompt;

    @Value("${spring.ai.azure.openai.chat.options.model}")
    private String deploymentOrModelId;

    @Value("classpath:/prompts/first-prompt.st")
    private Resource firstPrompt;

    private final MovieService movieService;

    private final OpenAIClient openAIClient;

    @Autowired
    public AzureChatService(PersistMessagesService persistMessagesService,
                           MovieService movieService,
                            OpenAIClient openAIClient) {
        this.persistMessagesService = persistMessagesService;
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        tokenizer = registry.getEncoding(EncodingType.CL100K_BASE);
        this.movieService = movieService;
        this.openAIClient = openAIClient;
    }

    public Flux<String> getCompletionStream(List<ChatCompletionMessage> history, String userIp) {
        List<String> logs = new ArrayList<>();
        if (history.isEmpty()) {
            return Flux.error(new RuntimeException("History is empty"));
        }
        List<ChatCompletionMessage> additionalMessages = getAdditionalPrompt(history, userIp);
        var question = history.get(history.size() - 1).getContent();
        if (question.equalsIgnoreCase("HELP")) {
            String helpMessage= "* **PORTAL** - Link to how you can deploy Vee yourself \n * **CREDITS** - The creators of Vee \n * **RESOURCES** - Key resources covered in the talk \n";
            return Flux.just("", helpMessage);
        } else if (question.equalsIgnoreCase("PORTAL")) {
            String portalMessage= "[https://portal.movee.dev](https://portal.movee.dev)\n";
            return Flux.just("", portalMessage);
        } else if (question.equalsIgnoreCase("CREDITS")) {
            String creditMessage = "* **Rohan Mukesh** - [LinkedIn](https://www.linkedin.com/in/rohan-mukesh-61ba74b/) \n * **Ryan Clair** - [LinkedIn](https://www.linkedin.com/in/ryanclair/) \n";
            return Flux.just("", creditMessage);
        } else if (question.equalsIgnoreCase("RESOURCES")) {
            String resourceMessage = "* Tree Of Thought paper: [link](https://arxiv.org/abs/2305.10601)\n * Tree Of Thought prompt examples: [link](https://github.com/dave1010/tree-of-thought-prompting/blob/main/tree-of-thought-prompts.txt)\n * Part Time Larry Embeddings video: [link](https://www.youtube.com/watch?v=xzHhZh7F25I)\n * Embeddings picture source: [link](https://www.featureform.com/post/the-definitive-guide-to-embeddings)\n * **Deploy Vee yourself**: [https://portal.movee.dev/](https://portal.movee.dev/)\n * **Have questions on Azure Spring Apps Enterprise?**: [Contact Us](mailto:tanzu-azure.pdl@broadcom.com?subject=AI%20Event%20Question) \n    * Tanzu-Azure.pdl@broadcom.com";
            return Flux.just("", resourceMessage);
        }

        if (history.size() == 1) {
            PromptTemplate promptTemplate = new PromptTemplate(firstPrompt);
            Prompt prompt = promptTemplate.create(Map.of("message", history.get(0).getContent()));
            logs.add("Sending model the prompt: ***"+prompt.getContents()+"***");
            ChatCompletionMessage augmentedUserMessage = new ChatCompletionMessage(Role.USER, prompt.getContents());
            additionalMessages.remove(1);
            additionalMessages.add(augmentedUserMessage);
        } else {
            logs.add("Sending model the prompt: ***"+question+"***");
        }
        logs.add("Azure OpenAI is using chat deployment model: ***" +deploymentOrModelId+"***");
        List<ChatRequestMessage> messages  = additionalMessages.stream().map(ChatMessageAdapter::from).toList();
        MovieFunctions movieFunctions = new MovieFunctions(movieService);
        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(messages);
        chatCompletionsOptions.setTools(movieFunctions.openAIFunctions());
        chatCompletionsOptions.setStream(true);
        IterableStream<ChatCompletions> chatCompletionsStream = null;
        try {
            chatCompletionsStream = openAIClient.getChatCompletionsStream(deploymentOrModelId,
                    chatCompletionsOptions);
        } catch (Exception e) {
            logs.add(e.getMessage());
            log.error(e.getMessage());
            return Flux.just("", "The response was filtered due to the prompt triggering Azure OpenAI's content management policy. Please modify your prompt and retry.");
        }
        AtomicReference<CompletionsFinishReason> finishReason = new AtomicReference<>();
        ChatFunctionDetails functionDetails = new ChatFunctionDetails();
        List<String> outputData = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        chatCompletionsStream.stream()
                .skip(1L)
                .forEach(chatCompletions -> {
                    ChatChoice choice = chatCompletions.getChoices().get(0);
                    if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                        outputData.add(choice.getDelta().getContent());
                    }
                    if (choice.getFinishReason() != null) {
                        finishReason.set(choice.getFinishReason());
                    }
                    List<ChatCompletionsToolCall> toolCalls = choice.getDelta().getToolCalls();
                    if (toolCalls != null) {
                        ChatCompletionsFunctionToolCall toolCall = (ChatCompletionsFunctionToolCall) toolCalls.get(0);
                        if (toolCall != null) {
                            functionDetails.getFunctionArguments().append(toolCall.getFunction().getArguments());
                            if (toolCall.getId() != null) {
                                functionDetails.setToolID(toolCall.getId());
                            }
                            if (toolCall.getFunction().getName() != null) {
                                functionDetails.setFunctionName(toolCall.getFunction().getName());
                            }
                        }
                    }
                    resultMap.put("outputData", outputData);
                });
        List<ChatRequestMessage> followUpMessages = new ArrayList<>(messages);
        Object functionCallResult = null;
        if (finishReason.get() == CompletionsFinishReason.TOOL_CALLS) {
            FunctionCall functionCall = new FunctionCall(functionDetails.getFunctionName(), functionDetails.getFunctionArguments().toString());
            ChatCompletionsFunctionToolCall functionToolCall = new ChatCompletionsFunctionToolCall(functionDetails.getToolID(), functionCall);
            ChatRequestAssistantMessage assistantRequestMessage = new ChatRequestAssistantMessage("");
            assistantRequestMessage.setToolCalls(List.of(functionToolCall));
            logs.add("The model needs more data and is running function: ***"+functionDetails.getFunctionName()+"***");
            if (functionCall.getName().equals("getMovieList")) {
                String prompt = "Imagine three brilliant, logical experts collaboratively answering the above question. Each one verbosely explains their thought process in real-time, considering the prior explanations of others and openly acknowledging mistakes. At each step, whenever possible, each expert refines and builds upon the thoughts of others, acknowledging their contributions. They continue until there is a definitive answer to the question. The final agreed upon answer should be given in a markdown numbered list format, in the format of ***{original_title}*** - ***Release Date***: {release_date} - ***Rating***:{avg_vote}. As a sub-bullet include ***Overview***: {overview}. NEVER truncate the results. List every movie in the markdown format.";
                ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(prompt);
                followUpMessages.add(chatRequestUserMessage);
                logs.add("Prompt Engineering strategy used: ***Tree of Thought***");
                logs.add("Augmented Prompt: ***"+prompt+"***");
                // Continue to maintain chatCompletionMessages
                ChatCompletionMessage userMessage = new ChatCompletionMessage(Role.USER, prompt);
                additionalMessages.add(userMessage);
            }

            if (functionCall.getName().equals("getSimilarMovies")) {
                String enhancedPrompt = "Generate an even better search prompt than my previous prompt. The search prompt will be used against an embedding datastore of combined movie synopsis and genres. Do not include the movie title. Reply back with ONLY the new search prompt. No AI commentary";
                ChatRequestUserMessage chatRequestEnhancedUserMessage = new ChatRequestUserMessage(enhancedPrompt);
                followUpMessages.add(chatRequestEnhancedUserMessage);
                logs.add("Creating one-shot Prompt: ***"+enhancedPrompt+"***");
                IterableStream<ChatCompletions> followUpChatCompletionsStream = openAIClient.getChatCompletionsStream(
                        deploymentOrModelId, new ChatCompletionsOptions(followUpMessages));
                List<String> modelPrompt = new ArrayList<>();
                followUpChatCompletionsStream.stream()
                        .skip(1L)
                        .forEach(chatCompletions -> {
                            ChatChoice choice = chatCompletions.getChoices().get(0);
                            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                modelPrompt.add(choice.getDelta().getContent());
                            }
                        });
                String prompt = modelPrompt.size() > 0 ? String.join("", modelPrompt): modelPrompt.toString();
                ChatRequestUserMessage chatRequestUserMessage = new ChatRequestUserMessage(prompt);
                followUpMessages.add(chatRequestUserMessage);
                logs.add("Model Generated Prompt: **"+prompt+"**");
                // Continue to maintain chatCompletionMessages
                ChatCompletionMessage userMessage = new ChatCompletionMessage(Role.USER, prompt);
                additionalMessages.add(userMessage);
                logs.add("The Embedding model used to search the user query: ***text-embedding-ada-002***");
                question = prompt;
            }
            functionCallResult = movieFunctions.executeFunctionCall(functionDetails, question, logs);
            logs.add("The tools call result: \n\n ```"+functionCallResult.toString()+"```");
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
            ChatCompletionMessage toolCompletionMessage = new ChatCompletionMessage(Role.TOOL, functionCallResult.toString(), functionDetails.getToolID());
            additionalMessages.add(toolCompletionMessage);
            IterableStream<ChatCompletions> followUpChatCompletionsStream = openAIClient.getChatCompletionsStream(
                    deploymentOrModelId, new ChatCompletionsOptions(followUpMessages));
            return Flux.fromStream(followUpChatCompletionsStream
                            .stream()
                            .skip(1)
                            .map(chatCompletions -> {
                                ChatChoice choice = chatCompletions.getChoices().get(0);
                                if(choice.getDelta().getContent() != null) {
                                    outputData.add(choice.getDelta().getContent());
                                    if (choice.getDelta().getContent().contains("\n\n") && choice.getFinishReason() != CompletionsFinishReason.STOPPED) {
                                        return choice.getDelta().getContent().replace("\n\n", "\n");
                                    }
                                    return choice.getDelta().getContent();
                                }
                                return "";
                            }))
                    .doAfterTerminate(() -> {
                        ChatCompletionMessage assistantMessage = new ChatCompletionMessage(Role.ASSISTANT, outputData.toString().replaceAll(",\\s", "").trim());
                        additionalMessages.add(assistantMessage);
                        logs.add("The model finished processing the message with: ***"+getTokenCount(additionalMessages)+"*** tokens");
                        persistMessagesService.persistMessages(additionalMessages, userIp);
                        persistMessagesService.persistLogs(logs, userIp);
                    }).onErrorResume(e -> {
                        return Mono.just("Error " + e.getMessage());
                    });
        } else if (finishReason.get() == CompletionsFinishReason.CONTENT_FILTERED) {
            return Flux.just("", "The response was filtered due to the prompt triggering Azure OpenAI's content management policy. Please modify your prompt and retry.");
        } else if (messages.size() == 2) {
            outputData.add(" I'm Vee, like your movie-savvy friend who knows too much, but without the annoying habit of talking during the film. I can help you with things like... \n **1. Show you what movies are in theaters (today or in the future), in a given city**.\n **2. Give movie synopses**.\n**3. Find similar movies**.\n For list of supported sub-commands, type **HELP**");
        }
        return Flux.fromIterable(outputData)
                .onErrorResume(e -> Mono.just("Error " + e.getMessage()))
                .doAfterTerminate(() -> {
                    ChatCompletionMessage assistantMessage = new ChatCompletionMessage(Role.ASSISTANT, outputData.toString().replaceAll(",\\s", "").trim());
                    additionalMessages.add(assistantMessage);
                    logs.add("The model finished processing the message with: ***"+getTokenCount(additionalMessages)+"*** tokens");
                    persistMessagesService.persistMessages(additionalMessages, userIp);
                    persistMessagesService.persistLogs(logs, userIp);
                });
    }

    public void savePrompt(String newPrompt, String userIp) {
        persistMessagesService.reset(userIp);
        persistMessagesService.persistPrompts(newPrompt, userIp);
    }

    private List<ChatCompletionMessage> getAdditionalPrompt(List<ChatCompletionMessage> history, String userIp) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var systemMessages = new ArrayList<ChatCompletionMessage>();
        String savedPrompt = persistMessagesService.getPrompts(userIp);
        if (savedPrompt != null) {
            systemMessages.add(new ChatCompletionMessage(
                    Role.SYSTEM,
                    savedPrompt
            ));
        } else {
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
            Prompt prompt = systemPromptTemplate.create(Map.of("date", dtf.format(LocalDateTime.now())));
            persistMessagesService.persistPrompts(prompt.getContents(), userIp);
            systemMessages.add(new ChatCompletionMessage(
                    Role.SYSTEM,
                    prompt.getContents()
            ));
        }


        return capMessages(systemMessages, history);
    }

    private List<ChatCompletionMessage> capMessages(List<ChatCompletionMessage> systemMessages,
                                                    List<ChatCompletionMessage> history) {
        var availableTokens = MAX_TOKENS - MAX_RESPONSE_TOKENS;
        var cappedHistory = new ArrayList<>(history);

        var tokens = getTokenCount(systemMessages) + getTokenCount(cappedHistory);

        while (tokens > availableTokens) {
            if (cappedHistory.size() == 1) {
                throw new RuntimeException("Cannot cap messages further, only user question left");
            }

            cappedHistory.remove(0);
            tokens = getTokenCount(systemMessages) + getTokenCount(cappedHistory);
        }

        var cappedMessages = new ArrayList<>(systemMessages);
        cappedMessages.addAll(cappedHistory);

        return cappedMessages;
    }


    /**
     * Returns the number of tokens in the messages.
     * @param messages The messages to count the tokens of
     * @return The number of tokens in the messages
     */
    private int getTokenCount(List<ChatCompletionMessage> messages) {
        var tokenCount = 3; // every reply is primed with <|start|>assistant<|message|>
        for (var message : messages) {
            tokenCount += getMessageTokenCount(message);
        }
        return tokenCount;
    }

    /**
     * Returns the number of tokens in the message.
     *
     * @param message The message to count the tokens of
     * @return The number of tokens in the message
     */
    private int getMessageTokenCount(ChatCompletionMessage message) {
        var tokens = 4; // every message follows <|start|>{role/name}\n{content}<|end|>\n

        tokens += tokenizer.encode(message.getRole().toString()).size();
        tokens += tokenizer.encode(message.getContent()).size();

        return tokens;
    }
}
