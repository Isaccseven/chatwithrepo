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
                        Package: %s
                        Classes: %s
                        Methods: %s
                        Dependencies: %s
                        Code Content:
                        ```java
                        %s
                        ```
                        """.formatted(
                        doc.getMetadata().get("filePath"),
                        doc.getMetadata().get("package"),
                        doc.getMetadata().get("classes"),
                        doc.getMetadata().get("methods"),
                        doc.getMetadata().get("dependencies"),
                        truncate(extractCodeContent(doc.getText()), 1000)))
                .collect(Collectors.joining("\n---\n"));
    }

    private String extractCodeContent(String fullText) {
        // Extract only the actual code content from the full text
        int contentIndex = fullText.indexOf("Content:");
        if (contentIndex != -1) {
            return fullText.substring(contentIndex + "Content:".length()).trim();
        }
        return fullText;
    }

    private String createPromptText(String query, String context) {
        return """
                You are analyzing a Java codebase. Here is the relevant code context:
                
                %s
                
                Based on this context, please answer the following question:
                %s
                
                Please provide a clear and concise answer, referencing specific parts of the code when relevant.
                If you need to show code examples, use markdown code blocks with the appropriate language tag.
                """.formatted(context, query);
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
