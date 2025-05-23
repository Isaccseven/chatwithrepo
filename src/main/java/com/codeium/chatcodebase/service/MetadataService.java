package com.codeium.chatcodebase.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MetadataService {

    public Map<String, Object> enhanceMetadata(AstService.AstDocument doc, int chunkIndex, int totalChunks) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", doc.getFilePath());
        metadata.put("package", doc.getMetadata().getPackageName());
        metadata.put("classes", doc.getMetadata().getClasses());
        metadata.put("methods", doc.getMetadata().getMethods());
        metadata.put("fields", doc.getMetadata().getFields());
        metadata.put("dependencies", doc.getMetadata().getDependencies());
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("totalChunks", totalChunks);
        return metadata;
    }
}
