package com.codeium.chatcodebase.service;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class DependencyToolConfig {

    private final DependencyService dependencyService;

    public DependencyToolConfig(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @JsonClassDescription("Request to get dependencies for a specific file")
    public record FileRequest(
            @JsonPropertyDescription("The path of the file to get dependencies for")
            String filePath
    ) {
    }

    public record FileDependencies(String fileName, List<String> dependencies) {
    }

    @Bean
    @Description("Get dependencies for a specific file")
    public Function<FileRequest, FileDependencies> getFileDependencies() {
        return request -> {
            var data = dependencyService.analyzeDependencies(List.of());
            var dependencies = data.nodes().stream()
                    .filter(node -> node.id().equals(request.filePath()))
                    .findFirst()
                    .map(node -> {
                        List<String> deps = data.links().stream()
                                .filter(link -> link.source().equals(node.id()))
                                .map(link -> data.nodes().stream()
                                        .filter(n -> n.id().equals(link.target()))
                                        .findFirst()
                                        .map(n -> n.name())
                                        .orElse("Unknown"))
                                .collect(Collectors.toList());
                        return new FileDependencies(node.name(), deps);
                    })
                    .orElse(new FileDependencies(request.filePath(), List.of()));

            log.info("Found dependencies for file {}: {}", request.filePath(), dependencies);
            return dependencies;
        };
    }

}
