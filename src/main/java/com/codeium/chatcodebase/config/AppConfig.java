package com.codeium.chatcodebase.config;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public GitLabApi gitLabApi(
        @Value("${gitlab.url}") String url,
        @Value("${gitlab.token}") String token
    ) {
        return new GitLabApi(url, token);
    }

    @Bean
    public ProjectApi projectApi(GitLabApi gitLabApi) {
        return gitLabApi.getProjectApi();
    }

    @Bean
    public RepositoryApi repositoryApi(GitLabApi gitLabApi) {
        return gitLabApi.getRepositoryApi();
    }
}
