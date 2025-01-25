package com.codeium.chatcodebase.service;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final VectorStoreService vectorStore;

    @Value("${spring.ai.system-prompt}")
    private String systemPrompt;

    public ChatService(ChatModel chatModel, VectorStoreService vectorStore) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
    }

    public String chatWithContext(String query) {
        List<Document> contextDocs = vectorStore.semanticSearch(query);
        String context = buildContextString(contextDocs);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(createPromptText(query, context))));

        return chatClient.prompt(prompt).call().chatResponse().getResult().getOutput().getText();
    }

    private String buildContextString(List<Document> docs) {
        return docs.stream()
                .map(doc -> """
                        File: %s
                        Classes: %s
                        Dependencies: %s
                        AST Excerpt: %s
                        """.formatted(
                        doc.getMetadata().get("filePath"),
                        doc.getMetadata().get("classes"),
                        doc.getMetadata().get("dependencies"),
                        truncate(doc.getText(), 500)))
                .collect(Collectors.joining("\n---\n"));
    }

    private String createPromptText(String query, String context) {
        return """
                Code Context:
                %s

                Question: %s
                Answer:""".formatted(context, query);
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
