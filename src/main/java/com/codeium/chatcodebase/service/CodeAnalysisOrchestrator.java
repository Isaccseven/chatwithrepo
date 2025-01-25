package com.codeium.chatcodebase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAnalysisOrchestrator {
    private final GitLabService gitLabService;
    private final AstService astService;
    private final VectorStoreService vectorStore;

    @Async
    public void analyzeRepository(String projectId) {
        try {
            List<GitLabService.CodeFile> files = gitLabService.fetchRepository(projectId);
            List<AstService.AstDocument> astDocs = astService.parseFiles(files);
            vectorStore.storeAstDocuments(astDocs);
        } catch (Exception e) {
            log.error("Repository analysis failed", e);
            throw new AnalysisException("Analysis failed for project: " + projectId, e);
        }
    }

    public static class AnalysisException extends RuntimeException {
        public AnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
