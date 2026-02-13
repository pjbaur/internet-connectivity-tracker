package me.paulbaur.ict;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for integration tests.
 * Provides Elasticsearch and Redis containers.
 */
@Configuration(proxyBeanMethods = false)
public class TestContainersConfig {

    private static final ElasticsearchContainer ELASTICSEARCH;
    private static final GenericContainer<?> REDIS;

    static {
        ELASTICSEARCH = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
            )
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");
        ELASTICSEARCH.start();

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Custom Elasticsearch properties used by the app
        registry.add("ict.elasticsearch.host", ELASTICSEARCH::getHost);
        registry.add("ict.elasticsearch.port", ELASTICSEARCH::getFirstMappedPort);
        registry.add("ict.elasticsearch.scheme", () -> "http");

        // Standard Spring Data Redis properties
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
