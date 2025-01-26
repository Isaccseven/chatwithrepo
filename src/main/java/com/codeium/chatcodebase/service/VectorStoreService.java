package com.codeium.chatcodebase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VectorStoreService {
    private final VectorStore vectorStore;

    @Value("${spring.ai.vectorstore.pgvector.collection:code-ast}")
    private String collectionName;

    public void storeAstDocuments(List<AstService.AstDocument> documents) {
        List<Document> aiDocs = documents.stream()
                .map(this::convertToAiDocument)
                .toList();

        vectorStore.add(aiDocs);
    }

    private Document convertToAiDocument(AstService.AstDocument doc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", doc.getFilePath());
        metadata.put("package", doc.getMetadata().getPackageName());
        metadata.put("classes", doc.getMetadata().getClasses());
        metadata.put("methods", doc.getMetadata().getMethods());
        metadata.put("fields", doc.getMetadata().getFields());
        metadata.put("dependencies", doc.getMetadata().getDependencies());
        metadata.put("astContent", doc.getAstContent());

        // Combine raw content with AST metadata for better semantic search
        String content = String.format("""
            File: %s
            Package: %s
            Classes: %s
            Methods: %s
            Dependencies: %s
            Content:
            %s
            """,
            doc.getFilePath(),
            doc.getMetadata().getPackageName(),
            String.join(", ", doc.getMetadata().getClasses()),
            String.join(", ", doc.getMetadata().getMethods()),
            String.join(", ", doc.getMetadata().getDependencies()),
            doc.getRawContent()
        );

        return new Document(content, metadata);
    }

    public List<Document> semanticSearch(String query) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(10)  // Increased from 5 to get more context
                .similarityThreshold(0.6)  // Slightly lowered for better recall
                .filterExpression("metadata.package IS NOT NULL")  // Only return valid Java files
                .build());
    }
}
