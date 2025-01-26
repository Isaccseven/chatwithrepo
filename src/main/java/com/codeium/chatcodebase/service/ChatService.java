package com.codeium.chatcodebase.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.ranking.DocumentRanker;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatService {
    private final ChatClient chatClient;
    private final VectorStoreService vectorStore;
    private final ChatModel chatModel;


    public ChatService(VectorStoreService vectorStore, ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    public String chatWithContext(String query) {
        String systemPrompt = """
                You are a senior Java developer assistant analyzing a codebase.
                Use the following code context to answer the user's question:
                
                {context}
                
                When referencing code, use specific file names, class names, and line numbers.
                Format code examples in markdown with appropriate language tags.
                Keep responses concise but informative, focusing on the most relevant parts of the codebase.
                """;

        Query chatQuery = new Query(query);

        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        Query transformedQuery = queryTransformer.transform(chatQuery);
        log.info("Transformed query: {}", transformedQuery.text());

        String response = chatClient.prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore.getVectorStore()))
                .user(transformedQuery.text())
                .system(systemPrompt)
                .call()
                .content();

        log.info("Chat response: {}", response);
        return response;
    }
}
