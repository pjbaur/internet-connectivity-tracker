package me.paulbaur.ict.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration using Redis as the cache provider.
 * Defines cache names and TTL settings for different cache regions.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * Configure cache manager with Redis backend.
     * Sets up different TTLs for different cache regions.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration with 60 second TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues();

        // Specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // probe-results: Cache recent probe results for 60 seconds
        cacheConfigurations.put("probe-results", defaultConfig.entryTtl(Duration.ofSeconds(60)));

        // target-status: Cache target status for 30 seconds
        cacheConfigurations.put("target-status", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // analytics: Cache analytics data for 5 minutes (if analytics is implemented later)
        cacheConfigurations.put("analytics", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("Configured Redis cache manager with {} cache regions", cacheConfigurations.size());

        return cacheManager;
    }
}
