package com.codeium.chatcodebase.service;

import org.springframework.stereotype.Service;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DependencyService {
    
    public DependencyData analyzeDependencies(List<AstService.AstDocument> documents) {
        Map<String, DependencyNode> nodes = new HashMap<>();
        Map<String, Set<String>> dependencyMap = new HashMap<>();
        Map<String, String> classToFileMap = new HashMap<>();
        
        // First pass: Create nodes for all files and collect their dependencies
        for (AstService.AstDocument doc : documents) {
            try {
                String filePath = doc.getFilePath();
                String fileId = filePath;
                
                // Add file node
                nodes.put(fileId, new DependencyNode(
                    fileId,
                    getSimpleFileName(filePath),
                    "file",
                    calculateFileSize(doc.getRawContent())
                ));
                
                // Store dependencies for this file
                dependencyMap.put(fileId, new HashSet<>());
                
                // Map package names to file paths for dependency resolution
                String packageName = doc.getMetadata().getPackageName();
                if (packageName != null && !packageName.isEmpty()) {
                    for (String className : doc.getMetadata().getClasses()) {
                        String fullClassName = packageName + "." + className;
                        classToFileMap.put(fullClassName, fileId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process document: {}", doc.getFilePath(), e);
            }
        }
        
        // Second pass: Create dependency links between files
        List<DependencyLink> links = new ArrayList<>();
        
        // Then create the dependency links
        for (AstService.AstDocument doc : documents) {
            try {
                String sourceFile = doc.getFilePath();
                
                // Process each import statement
                for (String dependency : doc.getMetadata().getDependencies()) {
                    String targetFile = classToFileMap.get(dependency);
                    if (targetFile != null && !targetFile.equals(sourceFile)) {
                        // Add to dependency map to avoid duplicates
                        dependencyMap.get(sourceFile).add(targetFile);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process dependencies for: {}", doc.getFilePath(), e);
            }
        }
        
        // Convert dependency map to links
        dependencyMap.forEach((source, targets) -> {
            targets.forEach(target -> {
                links.add(new DependencyLink(source, target, 1));
            });
        });
        
        return new DependencyData(new ArrayList<>(nodes.values()), links);
    }
    
    private int calculateFileSize(String content) {
        return Math.min(100, Math.max(20, content.length() / 100));
    }
    
    private String getSimpleFileName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash == -1 ? filePath : filePath.substring(lastSlash + 1);
    }
    
    private String getSimpleName(String fullyQualifiedName) {
        try {
            if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
                return "";
            }
            int lastDot = fullyQualifiedName.lastIndexOf('.');
            return lastDot == -1 ? fullyQualifiedName : fullyQualifiedName.substring(lastDot + 1);
        } catch (Exception e) {
            log.warn("Failed to get simple name from: {}", fullyQualifiedName, e);
            return "";
        }
    }
    
    public record DependencyNode(
        String id,
        String name,
        String type,
        int size
    ) {}
    
    public record DependencyLink(
        String source,
        String target,
        int value
    ) {}
    
    public record DependencyData(
        List<DependencyNode> nodes,
        List<DependencyLink> links
    ) {}
} 