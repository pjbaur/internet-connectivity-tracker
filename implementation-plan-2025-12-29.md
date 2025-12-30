 Perfect! I've created a comprehensive implementation plan organized into 6 parallel workstreams. The plan includes:

  Plan Highlights

  6 Parallel Workstreams:
  1. Observability & Monitoring - Actuator, Prometheus metrics, health indicators
  2. Resilience & Error Handling - Retry logic, circuit breakers, rate limiting
  3. Async Scheduling & ICMP - Thread pool configuration, ICMP probe implementation
  4. Event-Driven Architecture & Caching - Spring Events, Redis caching, API versioning
  5. Data Management & Analytics - Elasticsearch ILM, aggregate metrics, trend analysis
  6. Notifications & Multi-Node - Webhooks, distributed coordination, leader election

  Each workstream includes:
  - Files to create/modify with full paths
  - Step-by-step implementation guide
  - Configuration examples
  - Testing strategy
  - Dependencies and integration points

  The plan supports both parallel development (6 teams working simultaneously) and sequential implementation (suggested order: 1→2→3→4→5→6 for maximum value).

───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
 Ready to code?

 Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
 Implementation Plan: Internet Connectivity Tracker Enhancements

 Overview

 This plan implements ALL recommendations from the Claude assessment (docs/CLAUDE_ASSESSMENT_20251121.md), organized into
 6 parallel workstreams to enable concurrent development.

 User Requirements:
 - Implement all near-term, medium-term, and long-term recommendations
 - Add async thread pool configuration for scheduled probes
 - Implement ICMP probe strategy
 - Skip security implementation (acknowledged dev/homelab limitation)

 Current State:
 - Java 21, Spring Boot 3.3.0
 - Actuator dependency present but not configured
 - Comprehensive logging already in place (logback-spring.xml with JSON/Logstash)
 - Global exception handler exists
 - Single-threaded scheduler with TCP-only probes
 - Basic Elasticsearch integration without retry logic

 ---
 Workstream 1: Observability & Monitoring Infrastructure

 Priority: HIGH (Foundation for monitoring all features)
 Dependencies: None

 Scope

 Add Spring Boot Actuator health endpoints, Prometheus metrics, and custom health indicators.

 Files to Create

 1. src/main/java/me/paulbaur/ict/common/health/ElasticsearchHealthIndicator.java - ES connectivity health check
 2. src/main/java/me/paulbaur/ict/common/health/ProbeSchedulerHealthIndicator.java - Scheduler status tracking
 3. src/main/java/me/paulbaur/ict/common/metrics/ProbeMetrics.java - Micrometer metrics component
 4. src/main/java/me/paulbaur/ict/common/config/ActuatorConfig.java - Actuator endpoint configuration
 5. src/test/java/me/paulbaur/ict/common/health/ElasticsearchHealthIndicatorTest.java
 6. src/test/java/me/paulbaur/ict/common/metrics/ProbeMetricsIntegrationTest.java

 Files to Modify

 1. pom.xml - Add micrometer-registry-prometheus dependency
 2. src/main/resources/application.yml - Configure actuator endpoints and metrics
 3. src/main/java/me/paulbaur/ict/probe/service/ProbeServiceImpl.java - Record probe metrics
 4. src/main/java/me/paulbaur/ict/probe/service/ElasticProbeRepository.java - Record repository operation metrics

 Implementation Steps

 1. Add Micrometer Prometheus dependency to pom.xml
 2. Configure actuator in application.yml (expose health, metrics, prometheus endpoints)
 3. Create ElasticsearchHealthIndicator - checks cluster health via ES client
 4. Create ProbeSchedulerHealthIndicator - reports DOWN if no probes in 10 seconds
 5. Create ProbeMetrics component with counters (probe.executions.total) and timers (probe.latency)
 6. Instrument ProbeServiceImpl to record metrics on each probe execution
 7. Instrument ElasticProbeRepository to track ES operation success/failure rates
 8. Test with /actuator/health, /actuator/metrics, /actuator/prometheus endpoints

 Configuration (application.yml)

 management:
   endpoints:
     web:
       exposure:
         include: health,info,metrics,prometheus
       base-path: /actuator
   endpoint:
     health:
       show-details: always
   metrics:
     tags:
       application: ${spring.application.name}

 ---
 Workstream 2: Resilience & Error Handling

 Priority: HIGH (Critical for production reliability)
 Dependencies: None (benefits from WS1 metrics but not required)

 Scope

 Add retry logic, circuit breakers (Resilience4j), and rate limiting for API endpoints.

 Files to Create

 1. src/main/java/me/paulbaur/ict/common/config/ResilienceConfig.java - Resilience4j configuration
 2. src/main/java/me/paulbaur/ict/common/resilience/RetryableElasticsearchClient.java - ES client wrapper with retry
 3. src/main/java/me/paulbaur/ict/common/web/RateLimitFilter.java - Token bucket rate limiting
 4. src/main/java/me/paulbaur/ict/common/exception/RateLimitExceededException.java
 5. src/main/java/me/paulbaur/ict/common/exception/CircuitBreakerOpenException.java
 6. src/test/java/me/paulbaur/ict/common/resilience/RetryableElasticsearchClientTest.java

 Files to Modify

 1. pom.xml - Add Resilience4j and Bucket4j dependencies
 2. src/main/resources/application.yml - Configure retry policies and circuit breakers
 3. src/main/java/me/paulbaur/ict/common/config/ElasticsearchConfig.java - Wrap client with retry logic
 4. src/main/java/me/paulbaur/ict/probe/service/ElasticProbeRepository.java - Use retryable client
 5. src/main/java/me/paulbaur/ict/common/api/GlobalExceptionHandler.java - Handle RateLimitExceededException (429)
 6. src/main/java/me/paulbaur/ict/probe/service/strategy/TcpProbeStrategy.java - Add retry for transient failures

 Implementation Steps

 1. Add Maven dependencies: resilience4j-spring-boot3, resilience4j-retry, resilience4j-circuitbreaker, bucket4j-core
 2. Configure Resilience4j in application.yml (retry: maxAttempts=3, circuit breaker: failureRate=50%)
 3. Create RetryableElasticsearchClient that wraps ElasticsearchClient with Retry + CircuitBreaker decorators
 4. Update ElasticsearchConfig to provide retryable client bean
 5. Create RateLimitFilter using Bucket4j (100 requests/minute) for /api/** endpoints
 6. Add exception handlers for rate limiting and circuit breaker to GlobalExceptionHandler
 7. Apply retry to TcpProbeStrategy for SocketTimeoutException (not ConnectException)
 8. Test with TestContainers - stop/start ES to verify retry/circuit breaker behavior

 Configuration (application.yml)

 resilience4j:
   retry:
     instances:
       elasticsearch:
         maxAttempts: 3
         waitDuration: 500ms
   circuitbreaker:
     instances:
       elasticsearch:
         slidingWindowSize: 10
         failureRateThreshold: 50
         waitDurationInOpenState: 10s

 ---
 Workstream 3: Async Scheduling & ICMP Probe Strategy

 Priority: MEDIUM-HIGH (Improves throughput and adds requested feature)
 Dependencies: None

 Scope

 Configure thread pool for async probe execution and implement ICMP probe strategy.

 Files to Create

 1. src/main/java/me/paulbaur/ict/common/config/SchedulingConfig.java - ThreadPoolTaskExecutor configuration
 2. src/main/java/me/paulbaur/ict/probe/service/strategy/IcmpProbeStrategy.java - ICMP ping implementation
 3. src/main/java/me/paulbaur/ict/probe/service/strategy/ProbeStrategyFactory.java - Strategy selector
 4. src/main/java/me/paulbaur/ict/probe/domain/ProbeConfig.java - Per-target probe configuration
 5. src/test/java/me/paulbaur/ict/probe/service/strategy/IcmpProbeStrategyTest.java

 Files to Modify

 1. src/main/resources/application.yml - Add thread pool and ICMP timeout configuration
 2. src/main/java/me/paulbaur/ict/IctApplication.java - Add @EnableAsync annotation
 3. src/main/java/me/paulbaur/ict/probe/service/ProbeScheduler.java - Add @Async to executeProbes()
 4. src/main/java/me/paulbaur/ict/probe/service/ProbeServiceImpl.java - Use ProbeStrategyFactory
 5. src/main/java/me/paulbaur/ict/target/domain/Target.java - Add probeMethod and timeoutMs fields

 Implementation Steps

 1. Create SchedulingConfig with ThreadPoolTaskExecutor (corePoolSize=4, maxPoolSize=10)
 2. Add @EnableAsync to IctApplication.java
 3. Annotate ProbeScheduler.executeProbes() with @Async
 4. Implement IcmpProbeStrategy using ProcessBuilder to execute native ping command
   - Parse ping output to extract latency (handle Windows vs Linux/macOS differences)
   - Return ProbeResult with ProbeMethod.ICMP
 5. Create ProbeStrategyFactory that returns TcpProbeStrategy or IcmpProbeStrategy based on ProbeMethod
 6. Update Target domain to include ProbeMethod field (default TCP) and optional timeout
 7. Update ProbeServiceImpl to select strategy via factory based on target.probeMethod
 8. Update targets.yml schema to support probeMethod and timeoutMs fields
 9. Add thread pool metrics tracking (if WS1 completed)
 10. Test async execution - verify multiple probes run concurrently

 Configuration (application.yml)

 ict:
   probe:
     async:
       enabled: true
       core-pool-size: 4
       max-pool-size: 10
       queue-capacity: 100
     tcp:
       timeout-ms: 1000
     icmp:
       timeout-ms: 2000
       packet-size: 32

 ---
 Workstream 4: Event-Driven Architecture & Caching

 Priority: MEDIUM (Architectural improvement, prepares for scaling)
 Dependencies: None (integrates with WS6 notifications)

 Scope

 Decouple probe execution from storage using Spring Events, add Redis caching layer, implement API versioning.

 Files to Create

 1. src/main/java/me/paulbaur/ict/probe/event/ProbeResultEvent.java - Domain event
 2. src/main/java/me/paulbaur/ict/probe/event/ProbeResultEventPublisher.java - Event publisher
 3. src/main/java/me/paulbaur/ict/probe/event/listener/ElasticsearchEventListener.java - Async ES storage
 4. src/main/java/me/paulbaur/ict/probe/event/listener/CacheInvalidationEventListener.java - Cache eviction
 5. src/main/java/me/paulbaur/ict/common/config/RedisConfig.java - Redis configuration
 6. src/main/java/me/paulbaur/ict/common/config/CacheConfig.java - Spring Cache setup
 7. src/main/java/me/paulbaur/ict/probe/api/v1/ProbeControllerV1.java - Versioned API
 8. src/test/java/me/paulbaur/ict/probe/event/ProbeEventIntegrationTest.java

 Files to Modify

 1. pom.xml - Add spring-boot-starter-data-redis, spring-boot-starter-cache
 2. src/main/resources/application.yml - Configure Redis and cache TTLs
 3. src/main/java/me/paulbaur/ict/probe/service/ProbeServiceImpl.java - Publish events instead of direct saves
 4. src/main/java/me/paulbaur/ict/probe/api/ProbeController.java - Add @Cacheable, deprecate
 5. docker-compose.yml - Add Redis service

 Implementation Steps

 1. Add Redis and Cache dependencies to pom.xml
 2. Add Redis service to docker-compose.yml (redis:7-alpine on port 6379)
 3. Configure Redis in application.yml (spring.data.redis.host, cache TTL settings)
 4. Create ProbeResultEvent record with timestamp, result, isStateChange flag
 5. Create ProbeResultEventPublisher that compares current vs previous status to detect state changes
 6. Create ElasticsearchEventListener with @Async and @EventListener - saves to repository
 7. Create CacheInvalidationEventListener - evicts cache entries on new probe results
 8. Configure CacheConfig with @EnableCaching and define cache names (probe-results, target-status)
 9. Update ProbeServiceImpl to publish events instead of directly calling repository.save()
 10. Add @Cacheable to ProbeController.getRecentResults() with 60s TTL
 11. Copy ProbeController to ProbeControllerV1 mapped to /api/v1/probes/**
 12. Deprecate original controller or remap to /api/v2/probes/**
 13. Test with TestContainers Redis - verify async event processing and cache behavior

 Configuration (application.yml)

 spring:
   data:
     redis:
       host: ${REDIS_HOST:localhost}
       port: 6379
   cache:
     type: redis
     redis:
       time-to-live: 60000

 ---
 Workstream 5: Data Management & Historical Analytics

 Priority: MEDIUM-LOW (Operational improvement)
 Dependencies: None (benefits from WS4 events but not required)

 Scope

 Implement Elasticsearch Index Lifecycle Management (ILM), aggregate metrics, trend analysis APIs.

 Files to Create

 1. src/main/java/me/paulbaur/ict/common/config/ElasticsearchIndexConfig.java - ILM policy initialization
 2. src/main/java/me/paulbaur/ict/analytics/service/ProbeAnalyticsService.java - Aggregation logic
 3. src/main/java/me/paulbaur/ict/analytics/domain/AggregatedMetrics.java - Uptime/latency metrics records
 4. src/main/java/me/paulbaur/ict/analytics/api/AnalyticsController.java - Analytics REST API
 5. src/main/java/me/paulbaur/ict/analytics/repository/ElasticsearchAnalyticsRepository.java - ES aggregations
 6. src/test/java/me/paulbaur/ict/analytics/service/ProbeAnalyticsServiceTest.java

 Files to Modify

 1. src/main/resources/application.yml - Configure ILM settings
 2. src/main/java/me/paulbaur/ict/common/config/ElasticsearchConfig.java - Initialize ILM on startup

 Implementation Steps

 1. Create ElasticsearchIndexConfig component to initialize ILM policy on app startup
 2. Define ILM policy: Hot (7 days) → Warm (30 days) → Cold (90 days) → Delete
 3. Configure rollover based on size (50GB) or age (1 day)
 4. Create index template for probe-results-* pattern with explicit mappings
 5. Implement ProbeAnalyticsService with methods:
   - calculateUptime(targetId, start, end) - uptime percentage
   - calculateLatency(targetId, start, end, bucket) - average latency by time bucket
   - findStateChanges(targetId, start, end) - UP/DOWN transitions
 6. Create ElasticsearchAnalyticsRepository using ES aggregation API:
   - terms bucket by targetId
   - filters sub-aggregation for UP/DOWN counts
   - avg latency sub-aggregation
   - date_histogram for time series
 7. Implement AnalyticsController with endpoints:
   - GET /api/v1/analytics/uptime/{targetId}
   - GET /api/v1/analytics/latency/{targetId}
   - GET /api/v1/analytics/state-changes/{targetId}
 8. Apply caching with 5-minute TTL (if WS4 completed)
 9. Test with TestContainers - seed data and verify aggregations

 Configuration (application.yml)

 ict:
   elasticsearch:
     ilm:
       enabled: true
       hot-phase-days: 7
       warm-phase-days: 30
       delete-phase-days: 90
       rollover-size-gb: 50

 ---
 Workstream 6: Notifications & Multi-Node Support

 Priority: LOW (Long-term enhancement)
 Dependencies: WS4 (events) required for notification triggers

 Scope

 Webhook notifications on state changes, distributed coordination for multi-node deployment.

 Files to Create

 1. src/main/java/me/paulbaur/ict/notification/service/NotificationService.java - Notification logic
 2. src/main/java/me/paulbaur/ict/notification/provider/WebhookNotificationProvider.java - HTTP webhook sender
 3. src/main/java/me/paulbaur/ict/notification/domain/NotificationConfig.java - Per-target notification settings
 4. src/main/java/me/paulbaur/ict/notification/event/listener/StateChangeNotificationListener.java - Event listener
 5. src/main/java/me/paulbaur/ict/common/config/RabbitMqConfig.java - Message queue setup (optional)
 6. src/main/java/me/paulbaur/ict/coordination/service/DistributedLockService.java - Redis-based locking
 7. src/main/java/me/paulbaur/ict/coordination/service/LeaderElectionService.java - Leader election

 Files to Modify

 1. pom.xml - Add spring-boot-starter-amqp, redisson-spring-boot-starter
 2. src/main/resources/application.yml - Configure notifications and coordination
 3. src/main/java/me/paulbaur/ict/probe/service/ProbeScheduler.java - Check leader status before probing
 4. docker-compose.yml - Add RabbitMQ service (optional)

 Implementation Steps

 1. Add AMQP and Redisson dependencies
 2. Implement NotificationService interface with webhook provider
 3. Implement WebhookNotificationProvider using RestTemplate/WebClient for HTTP POST
 4. Create StateChangeNotificationListener that listens for ProbeResultEvent where isStateChange=true
 5. Add notification filters (notify on UP→DOWN, DOWN→UP, skip repeated DOWN)
 6. Implement DistributedLockService using Redisson for Redis-based distributed locks
 7. Implement LeaderElectionService with Redis-based leader election and heartbeat
 8. Update ProbeScheduler to skip execution if not leader node
 9. (Optional) Add RabbitMQ for event broadcasting to external systems
 10. Test multi-node coordination - start 2-3 app instances, verify only leader probes

 Configuration (application.yml)

 ict:
   notifications:
     enabled: true
     providers:
       webhook:
         enabled: true
         timeout-ms: 5000
     filters:
       notify-on-up-to-down: true
       notify-on-down-to-up: true

 ---
 Maven Dependencies Summary

 Add to pom.xml:

 <!-- WS1: Observability -->
 <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-registry-prometheus</artifactId>
 </dependency>

 <!-- WS2: Resilience -->
 <dependency>
     <groupId>io.github.resilience4j</groupId>
     <artifactId>resilience4j-spring-boot3</artifactId>
     <version>2.1.0</version>
 </dependency>
 <dependency>
     <groupId>com.github.vladimir-bukhtoyarov</groupId>
     <artifactId>bucket4j-core</artifactId>
     <version>8.7.0</version>
 </dependency>

 <!-- WS4: Caching -->
 <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-data-redis</artifactId>
 </dependency>
 <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-cache</artifactId>
 </dependency>

 <!-- WS6: Coordination -->
 <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-amqp</artifactId>
 </dependency>
 <dependency>
     <groupId>org.redisson</groupId>
     <artifactId>redisson-spring-boot-starter</artifactId>
     <version>3.25.0</version>
 </dependency>

 ---
 Docker Compose Updates

 Add services to docker-compose.yml:

 # WS4: Redis for caching
 redis:
   image: redis:7-alpine
   ports:
     - "6379:6379"
   healthcheck:
     test: ["CMD", "redis-cli", "ping"]
     interval: 5s

 # WS6: RabbitMQ for messaging (optional)
 rabbitmq:
   image: rabbitmq:3-management-alpine
   ports:
     - "5672:5672"
     - "15672:15672"
   environment:
     RABBITMQ_DEFAULT_USER: ict
     RABBITMQ_DEFAULT_PASS: ict123

 ---
 Implementation Order (Sequential)

 If implementing one at a time (not in parallel):

 1. Workstream 1 (Observability) - Foundation for monitoring all features
 2. Workstream 2 (Resilience) - Critical for production stability
 3. Workstream 3 (Async & ICMP) - Improves performance + requested feature
 4. Workstream 4 (Events & Caching) - Architectural improvement
 5. Workstream 5 (Analytics) - Operational improvement
 6. Workstream 6 (Notifications) - Future enhancement

 ---
 Integration Points

 - WS1 → WS2: Circuit breaker state exposed via metrics
 - WS1 → WS3: Thread pool metrics (active threads, queue size)
 - WS2 → WS3: Apply retry to ICMP strategy
 - WS4 → WS5: Events trigger analytics updates
 - WS4 → WS6: Events trigger notifications
 - WS1 → WS6: Leader election status in health endpoint

 ---
 Critical Files Reference

 Most Modified Files

 - pom.xml - Dependencies for all workstreams
 - src/main/resources/application.yml - Configuration for all features
 - src/main/java/me/paulbaur/ict/probe/service/ProbeServiceImpl.java - Core probe logic (WS1, WS2, WS3, WS4)
 - src/main/java/me/paulbaur/ict/common/config/ElasticsearchConfig.java - ES configuration (WS2, WS5)
 - docker-compose.yml - Add Redis and RabbitMQ services

 Key Pattern Files to Follow

 - src/main/java/me/paulbaur/ict/probe/service/strategy/TcpProbeStrategy.java - Pattern for ICMP implementation
 - src/main/java/me/paulbaur/ict/target/domain/Target.java - Domain model to extend
 - src/main/java/me/paulbaur/ict/common/api/GlobalExceptionHandler.java - Exception handling pattern

 ---
 Testing Strategy

 Unit Tests

 - Mock external dependencies (ES, Redis, webhooks)
 - Achieve 80%+ code coverage
 - Test business logic in isolation

 Integration Tests

 - Use TestContainers (Elasticsearch already configured, add Redis)
 - Test cross-cutting concerns (events, caching, metrics)
 - WireMock for webhook testing

 End-to-End Tests

 - Full Docker Compose stack
 - Seed data → execute probes → verify pipeline
 - Test failure scenarios (ES down, Redis down)

 ---
 Feature Flags

 Add toggles for new features in application.yml:

 ict:
   features:
     icmp-probe: true
     caching: true
     notifications: false
     distributed-mode: false

 ---
 Rollback Strategy

 All features can be disabled via configuration without code changes:
 - WS1: management.endpoints.enabled-by-default=false
 - WS2: resilience4j.*.enabled=false
 - WS3: Revert to sync execution, disable ICMP via feature flag
 - WS4: Disable event publishing via feature flag
 - WS5: Disable ILM and analytics API
 - WS6: Run single-node mode, disable notifications
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
