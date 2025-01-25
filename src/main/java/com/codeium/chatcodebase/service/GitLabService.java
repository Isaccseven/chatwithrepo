package com.codeium.chatcodebase.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitLabService {
    private final ProjectApi projectApi;
    private final RepositoryApi repositoryApi;

    @Value("${gitlab.default-branch:main}")
    private String defaultBranch;

    public List<CodeFile> fetchRepository(String projectIdOrPath) {
        try {
            List<TreeItem> tree = getCompleteTree(projectIdOrPath);
            return processTreeNodes(projectIdOrPath, tree);
        } catch (GitLabApiException e) {
            throw new RepositoryFetchException("Failed to fetch repository", e);
        }
    }

    private List<TreeItem> getCompleteTree(String projectId) throws GitLabApiException {
        List<TreeItem> allNodes = new ArrayList<>();
        boolean recursive = true;

        // Get all files recursively in a single call
        List<TreeItem> nodes = repositoryApi.getTree(projectId, defaultBranch, "/", recursive);
        allNodes.addAll(nodes);
        
        return allNodes;
    }

    private List<CodeFile> processTreeNodes(String projectId, List<TreeItem> nodes) {
        return nodes.parallelStream()
                .filter(node -> node.getType() == TreeItem.Type.BLOB)
                .map(node -> processFile(projectId, node))
                .filter(Objects::nonNull)
                .toList();
    }

    private CodeFile processFile(String projectId, TreeItem node) {
        try {
            InputStream inputStream = repositoryApi.getRawBlobContent(projectId, node.getId());
            String content = readInputStream(inputStream);

            return new CodeFile(
                    node.getPath(),
                    content,
                    node.getMode(),
                    0L, // Default size since TreeItem doesn't provide it
                    node.getId(),
                    node.getId());
        } catch (GitLabApiException | IOException e) {
            log.error("Failed to process file: {}", node.getPath(), e);
            return null;
        }
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString();
    }

    public static record CodeFile(
            String path,
            String content,
            String mode,
            Long size,
            String commitId,
            String lastCommitId) {
    }

    public static class RepositoryFetchException extends RuntimeException {
        public RepositoryFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
