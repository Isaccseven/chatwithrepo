package com.codeium.chatcodebase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VectorStoreService {
    private final VectorStore vectorStore;
    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    @Value("${spring.ai.vectorstore.max-tokens:8000}")
    private int maxTokens;

    @Value("${spring.ai.vectorstore.chunk-size:6000}")
    private int chunkSize;

    @Value("${spring.ai.vectorstore.chunk-overlap:500}")
    private int chunkOverlap;

    private static final int BUFFER_SIZE = 8192;

    public void storeAstDocuments(List<AstService.AstDocument> documents) {
        // Process one document at a time to minimize memory usage
        for (AstService.AstDocument doc : documents) {
            try {
                List<Document> chunks = processDocument(doc);
                if (!chunks.isEmpty()) {
                    vectorStore.add(chunks);
                }
            } catch (Exception e) {
                // Log error but continue processing other documents
                log.error("Failed to process document: {}", doc.getFilePath(), e);
            }
        }
    }

    private List<Document> processDocument(AstService.AstDocument doc) {
        String content = doc.getRawContent();
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        // For small files, process as a single chunk
        if (content.length() / 4 <= chunkSize) {
            return List.of(convertToAiDocument(doc, content, 1, 1));
        }

        List<Document> documents = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder(chunkSize * 4);
        int chunkIndex = 1;
        int totalChunks = estimateTotalChunks(content.length());
        
        try (java.io.StringReader reader = new java.io.StringReader(content)) {
            char[] buffer = new char[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = reader.read(buffer)) != -1) {
                currentChunk.append(buffer, 0, bytesRead);
                
                // Process complete chunks
                while (currentChunk.length() >= chunkSize * 4) {
                    int endIndex = findChunkEndIndex(currentChunk);
                    // Create document directly without intermediate string storage
                    documents.add(convertToAiDocument(
                        doc,
                        currentChunk.substring(0, endIndex),
                        chunkIndex++,
                        totalChunks
                    ));
                    
                    // Keep overlap for next chunk
                    if (endIndex < currentChunk.length()) {
                        currentChunk.delete(0, Math.max(0, endIndex - (chunkOverlap * 4)));
                    } else {
                        currentChunk.setLength(0);
                    }
                }
            }
            
            // Process final chunk if any content remains
            if (currentChunk.length() > 0) {
                documents.add(convertToAiDocument(
                    doc,
                    currentChunk.toString(),
                    chunkIndex,
                    totalChunks
                ));
            }
        } catch (java.io.IOException e) {
            log.error("Failed to read content for file: {}", doc.getFilePath(), e);
            return List.of();
        }

        return documents;
    }

    private int estimateTotalChunks(int contentLength) {
        int chunkSizeInChars = chunkSize * 4;
        return (contentLength + chunkSizeInChars - 1) / chunkSizeInChars;
    }

    private int findChunkEndIndex(StringBuilder content) {
        int targetEnd = Math.min(content.length(), chunkSize * 4);
        int searchStart = Math.max(0, targetEnd - (chunkOverlap * 4));
        
        // Try to find natural boundaries
        for (String boundary : new String[]{"\n}", ";\n", "\n\n", "\n", ". "}) {
            int index = content.lastIndexOf(boundary, targetEnd);
            if (index >= searchStart) {
                return index + boundary.length();
            }
        }
        
        return targetEnd;
    }

    private Document convertToAiDocument(AstService.AstDocument doc, String chunkContent, int chunkIndex, int totalChunks) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", doc.getFilePath());
        metadata.put("package", doc.getMetadata().getPackageName());
        metadata.put("classes", doc.getMetadata().getClasses());
        metadata.put("methods", doc.getMetadata().getMethods());
        metadata.put("fields", doc.getMetadata().getFields());
        metadata.put("dependencies", doc.getMetadata().getDependencies());
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("totalChunks", totalChunks);

        // Create content with minimal string concatenation
        return new Document(
            new StringBuilder(chunkContent.length() + 200) // Pre-allocate buffer
                .append("File: ").append(doc.getFilePath())
                .append(" (").append(chunkIndex).append('/').append(totalChunks).append(")\n")
                .append("Package: ").append(doc.getMetadata().getPackageName()).append('\n')
                .append("Classes: ").append(String.join(", ", doc.getMetadata().getClasses())).append('\n')
                .append("Methods: ").append(String.join(", ", doc.getMetadata().getMethods())).append('\n')
                .append("Content:\n")
                .append(chunkContent)
                .append('\n')
                .toString(),
            metadata
        );
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
