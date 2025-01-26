package com.codeium.chatcodebase.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAnalysisOrchestrator {
    private final GitLabService gitLabService;
    private final AstService astService;
    private final VectorStoreService vectorStore;
    private final DependencyService dependencyService;
    
    private final ConcurrentHashMap<String, AnalysisStatus> analysisStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DependencyService.DependencyData> dependencyDataMap = new ConcurrentHashMap<>();

    @Async
    public void analyzeRepository(String projectId) {
        AnalysisStatus status = new AnalysisStatus();
        analysisStatusMap.put(projectId, status);
        
        try {
            // Step 1: Fetch repository files
            status.setCurrentStep(AnalysisStep.FETCHING_FILES);
            List<GitLabService.CodeFile> files = gitLabService.fetchRepository(projectId);
            status.setProgress(20);
            
            // Step 2: Parse AST
            status.setCurrentStep(AnalysisStep.PARSING_AST);
            List<AstService.AstDocument> astDocs = astService.parseFiles(files);
            status.setProgress(40);
            
            // Step 3: Analyze dependencies
            status.setCurrentStep(AnalysisStep.ANALYZING_DEPENDENCIES);
            DependencyService.DependencyData dependencyData = dependencyService.analyzeDependencies(astDocs);
            dependencyDataMap.put(projectId, dependencyData);
            status.setProgress(60);
            
            // Step 4: Generate embeddings and store
            status.setCurrentStep(AnalysisStep.STORING_VECTORS);
            vectorStore.storeAstDocuments(astDocs);
            status.setProgress(100);
            
            status.setCurrentStep(AnalysisStep.COMPLETED);
            status.setSuccess(true);
            
            log.info("Repository analysis completed successfully for project: {}", projectId);
        } catch (Exception e) {
            status.setError(e.getMessage());
            status.setSuccess(false);
            log.error("Repository analysis failed for project: {}", projectId, e);
            throw new AnalysisException("Analysis failed for project: " + projectId, e);
        }
    }
    
    public AnalysisStatus getAnalysisStatus(String projectId) {
        return analysisStatusMap.getOrDefault(projectId, 
            new AnalysisStatus(AnalysisStep.NOT_STARTED, 0, null, false));
    }
    
    public DependencyService.DependencyData getDependencyData(String projectId) {
        return dependencyDataMap.get(projectId);
    }
    
    @Getter
    public static class AnalysisStatus {
        private AnalysisStep currentStep;
        private final AtomicInteger progress;
        private String error;
        private boolean success;
        
        public AnalysisStatus() {
            this.currentStep = AnalysisStep.NOT_STARTED;
            this.progress = new AtomicInteger(0);
            this.success = false;
        }
        
        public AnalysisStatus(AnalysisStep step, int progress, String error, boolean success) {
            this.currentStep = step;
            this.progress = new AtomicInteger(progress);
            this.error = error;
            this.success = success;
        }
        
        public void setCurrentStep(AnalysisStep step) {
            this.currentStep = step;
        }
        
        public void setProgress(int progress) {
            this.progress.set(progress);
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
    
    public enum AnalysisStep {
        NOT_STARTED,
        FETCHING_FILES,
        PARSING_AST,
        ANALYZING_DEPENDENCIES,
        STORING_VECTORS,
        COMPLETED
    }

    public static class AnalysisException extends RuntimeException {
        public AnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
