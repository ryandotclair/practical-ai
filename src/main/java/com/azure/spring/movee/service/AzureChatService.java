package com.azure.spring.movee.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.azure.core.util.IterableStream;
import com.azure.spring.movee.adapter.ChatMessageAdapter;
import com.azure.spring.movee.model.ChatCompletionMessage;
import com.azure.spring.movee.model.ChatFunctionDetails;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.*;
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

    private final MovieFunctionService movieService;

    private final OpenAIClient openAIClient;

    @Autowired
    public AzureChatService(PersistMessagesService persistMessagesService,
                            MovieFunctionService movieService,
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
        Flux<String> helpCommands = getHelpCommands(question);
        if (helpCommands != null) return helpCommands;

        if (history.size() == 1) {
            PromptTemplate promptTemplate = new PromptTemplate(firstPrompt);
            Prompt prompt = promptTemplate.create(Map.of("message", history.get(0).getContent()));
            logs.add("Sending model the prompt: ***" + prompt.getContents() + "***");
            ChatCompletionMessage augmentedUserMessage = new ChatCompletionMessage(Role.USER, prompt.getContents());
            additionalMessages.remove(1);
            additionalMessages.add(augmentedUserMessage);
        } else {
            logs.add("Sending model the prompt: ***" + question + "***");
        }
        logs.add("Azure OpenAI is using chat deployment model: ***" + deploymentOrModelId + "***");
        List<ChatRequestMessage> messages = additionalMessages.stream().map(ChatMessageAdapter::from).toList();
        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(messages);
        chatCompletionsOptions.setTools(movieService.openAIFunctions());
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
                                }});
        List<ChatRequestMessage> followUpMessages = new ArrayList<>(messages);
        if (finishReason.get() == CompletionsFinishReason.TOOL_CALLS) {
            return Flux.fromStream(movieService.executeFunctionTools(userIp, functionDetails, logs, followUpMessages, additionalMessages, question, outputData))
                    .doAfterTerminate(() -> {
                        persistMessages(userIp, outputData, additionalMessages, logs);
                    }).onErrorResume(e -> Mono.just("Error " + e.getMessage()));
        } else if (finishReason.get() == CompletionsFinishReason.CONTENT_FILTERED) {
            return Flux.just("", "The response was filtered due to the prompt triggering Azure OpenAI's content management policy. Please modify your prompt and retry.");
        } else if (messages.size() == 2) {
            outputData.add(" I'm Vee, like your movie-savvy friend who knows too much, but without the annoying habit of talking during the film. I can help you with things like... \n **1. Show you what movies are in theaters (today or in the future), in a given city**.\n **2. Give movie synopses**.\n**3. Find similar movies**.\n For list of supported sub-commands, type **HELP**");
        }
        return Flux.fromIterable(outputData)
                .onErrorResume(e -> Mono.just("Error " + e.getMessage()))
                .doAfterTerminate(() -> persistMessages(userIp, outputData, additionalMessages, logs));
    }


    private void persistMessages(String userIp, List<String> outputData, List<ChatCompletionMessage> additionalMessages, List<String> logs) {
        ChatCompletionMessage assistantMessage = new ChatCompletionMessage(Role.ASSISTANT, outputData.toString().replaceAll(",\\s", "").trim());
        additionalMessages.add(assistantMessage);
        logs.add("The model finished processing the message with: ***" + getTokenCount(additionalMessages) + "*** tokens");
        persistMessagesService.persistMessages(additionalMessages, userIp);
        persistMessagesService.persistLogs(logs, userIp);
    }

    private Flux<String> getHelpCommands(String question) {
        if (question.equalsIgnoreCase("HELP")) {
            String helpMessage = "* **PORTAL** - Link to how you can deploy Vee yourself \n * **CREDITS** - The creators of Vee \n * **RESOURCES** - Key resources covered in the talk \n";
            return Flux.just("", helpMessage);
        } else if (question.equalsIgnoreCase("PORTAL")) {
            String portalMessage = "[https://portal.movee.dev](https://portal.movee.dev)\n";
            return Flux.just("", portalMessage);
        } else if (question.equalsIgnoreCase("CREDITS")) {
            String creditMessage = "* **Rohan Mukesh** - [LinkedIn](https://www.linkedin.com/in/rohan-mukesh-61ba74b/) \n * **Ryan Clair** - [LinkedIn](https://www.linkedin.com/in/ryanclair/) \n";
            return Flux.just("", creditMessage);
        } else if (question.equalsIgnoreCase("RESOURCES")) {
            String resourceMessage = "* Tree Of Thought paper: [link](https://arxiv.org/abs/2305.10601)\n * Tree Of Thought prompt examples: [link](https://github.com/dave1010/tree-of-thought-prompting/blob/main/tree-of-thought-prompts.txt)\n * Part Time Larry Embeddings video: [link](https://www.youtube.com/watch?v=xzHhZh7F25I)\n * Embeddings picture source: [link](https://www.featureform.com/post/the-definitive-guide-to-embeddings)\n * **Deploy Vee yourself**: [https://portal.movee.dev/](https://portal.movee.dev/)\n * **Have questions on Azure Spring Apps Enterprise?**: [Contact Us](mailto:tanzu-azure.pdl@broadcom.com?subject=AI%20Event%20Question) \n    * Tanzu-Azure.pdl@broadcom.com";
            return Flux.just("", resourceMessage);
        }
        return null;
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
     *
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
