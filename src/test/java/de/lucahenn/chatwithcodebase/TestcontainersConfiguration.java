package de.lucahenn.chatwithcodebase;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection
    GenericContainer<?> ollamaContainer() {
        return new GenericContainer<>(DockerImageName.parse("ollama/ollama:latest"))
                .withExposedPorts(11434);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection
    PostgreSQLContainer<?> pgvectorContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    }
}
