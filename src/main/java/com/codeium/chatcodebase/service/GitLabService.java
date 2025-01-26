package com.codeium.chatcodebase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.RepositoryFileApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitLabService {
    private final GitLabApi gitLabApi;

    @Value("${gitlab.default-branch:main}")
    private String defaultBranch;

    private static final int MAX_FILE_SIZE_BYTES = 100_000; // 100KB limit to prevent token overflow
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".java", ".kt", ".py", ".js", ".ts", ".jsx", ".tsx", 
            ".xml", ".yml", ".yaml", ".json", ".md", ".txt"
    );

    public List<CodeFile> fetchRepository(String projectIdOrPath) {
        try {
            log.debug("Attempting to fetch repository for project: {}", projectIdOrPath);
            
            // First verify the project exists and is accessible
            try {
                gitLabApi.getProjectApi().getProject(projectIdOrPath);
                log.debug("Successfully verified project access");
            } catch (GitLabApiException e) {
                log.error("Failed to access project: {}. Error: {}", projectIdOrPath, e.getMessage());
                throw new RepositoryFetchException("Project not found or not accessible", e);
            }
            
            List<TreeItem> tree = getCompleteTree(projectIdOrPath);
            return processTreeNodes(projectIdOrPath, tree);
        } catch (GitLabApiException e) {
            throw new RepositoryFetchException("Failed to fetch repository", e);
        }
    }

    private List<TreeItem> getCompleteTree(String projectId) throws GitLabApiException {
        log.error("Starting repository tree fetch for project: {}", projectId);
        
        // First try to get the default branch if it's not set correctly
        try {
            var project = gitLabApi.getProjectApi().getProject(projectId);
            String actualDefaultBranch = project.getDefaultBranch();
            log.error("Project info - ID: {}, Name: {}, Default Branch: {}", 
                project.getId(), project.getName(), actualDefaultBranch);
            
            if (actualDefaultBranch == null || actualDefaultBranch.isEmpty()) {
                log.error("Project has no default branch, it might be empty");
                return new ArrayList<>();
            }
            
            if (!actualDefaultBranch.equals(defaultBranch)) {
                log.error("Using project's default branch: {} instead of configured branch: {}", actualDefaultBranch, defaultBranch);
                defaultBranch = actualDefaultBranch;
            }

            // Get repository tree with recursive flag
            log.error("Attempting to get repository tree recursively");
            List<TreeItem> items = gitLabApi.getRepositoryApi().getTree(projectId, null, defaultBranch, true);
            if (items == null || items.isEmpty()) {
                log.error("No files found in repository");
                return new ArrayList<>();
            }
            
            log.error("Successfully retrieved {} files from tree", items.size());
            return items;
            
        } catch (GitLabApiException e) {
            log.error("Failed to get repository files: {} - {}", e.getMessage(), e.getHttpStatus());
            throw e;
        }
    }

    private List<CodeFile> processTreeNodes(String projectId, List<TreeItem> nodes) {
        return nodes.parallelStream()
                .filter(node -> node.getType() == TreeItem.Type.BLOB) // Only process files, skip directories
                .filter(this::isFileSupported) // Only process supported file types
                .map(node -> processFile(projectId, node))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isFileSupported(TreeItem node) {
        String path = node.getPath().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(path::endsWith) &&
               !path.contains("/test/") && // Skip test files
               !path.contains("/tests/") &&
               !path.contains("/generated/"); // Skip generated files
    }

    private CodeFile processFile(String projectId, TreeItem node) {
        try {
            // Get file content using repository file API with correct parameter order
            RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile(projectId, node.getPath(), defaultBranch);
            if (file == null) {
                log.error("File not found: {}", node.getPath());
                return null;
            }
            
            // Get the content and decode from base64
            String content = file.getDecodedContentAsString();
            
            // Double check content size after decoding
            if (content.length() > MAX_FILE_SIZE_BYTES) {
                log.warn("File too large after decoding: {} ({} bytes)", node.getPath(), content.length());
                return null;
            }

            return new CodeFile(
                    node.getPath(),
                    content,
                    node.getMode()
            );
        } catch (GitLabApiException e) {
            log.error("Failed to process file: {}", node.getPath(), e);
            return null;
        }
    }

    public static record CodeFile(
            String path,
            String content,
            String mode
            ) {
    }

    public static class RepositoryFetchException extends RuntimeException {
        public RepositoryFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
