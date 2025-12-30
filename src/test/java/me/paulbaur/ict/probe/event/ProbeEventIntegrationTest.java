package me.paulbaur.ict.probe.event;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.event.listener.CacheInvalidationEventListener;
import me.paulbaur.ict.probe.event.listener.ElasticsearchEventListener;
import me.paulbaur.ict.probe.service.ProbeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for probe result events.
 * Tests the event-driven architecture from publisher to listeners.
 */
@ExtendWith(MockitoExtension.class)
class ProbeEventIntegrationTest {

    @Mock
    private ProbeRepository probeRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache probeResultsCache;

    @Mock
    private Cache targetStatusCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProbeResultEventPublisher publisher;
    private ElasticsearchEventListener elasticsearchListener;
    private CacheInvalidationEventListener cacheListener;

    @BeforeEach
    void setUp() {
        publisher = new ProbeResultEventPublisher(eventPublisher, probeRepository);
        elasticsearchListener = new ElasticsearchEventListener(probeRepository);
        cacheListener = new CacheInvalidationEventListener(cacheManager);
    }

    @Test
    void publishProbeResult_shouldPublishEvent() {
        // Given
        ProbeResult result = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                42L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        when(probeRepository.findRecent("target-123", 1)).thenReturn(List.of());

        // When
        publisher.publishProbeResult(result);

        // Then
        ArgumentCaptor<ProbeResultEvent> eventCaptor = ArgumentCaptor.forClass(ProbeResultEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ProbeResultEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getResult()).isEqualTo(result);
        assertThat(capturedEvent.isStateChange()).isFalse(); // First result, not a state change
    }

    @Test
    void publishProbeResult_detectsStateChange() {
        // Given
        ProbeResult previousResult = new ProbeResult(
                Instant.now().minusSeconds(10),
                "target-123",
                "example.com",
                42L,
                "cycle-122",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        ProbeResult newResult = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                null,
                "cycle-123",
                ProbeStatus.DOWN,
                ProbeMethod.TCP,
                "connection refused"
        );

        when(probeRepository.findRecent("target-123", 1)).thenReturn(List.of(previousResult));

        // When
        publisher.publishProbeResult(newResult);

        // Then
        ArgumentCaptor<ProbeResultEvent> eventCaptor = ArgumentCaptor.forClass(ProbeResultEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ProbeResultEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getResult()).isEqualTo(newResult);
        assertThat(capturedEvent.isStateChange()).isTrue(); // UP -> DOWN is a state change
    }

    @Test
    void elasticsearchListener_shouldSaveResult() {
        // Given
        ProbeResult result = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                42L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        ProbeResultEvent event = new ProbeResultEvent(this, result, false);

        // When
        elasticsearchListener.handleProbeResultEvent(event);

        // Then
        verify(probeRepository).save(result);
    }

    @Test
    void cacheListener_shouldInvalidateCaches() {
        // Given
        ProbeResult result = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                42L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        ProbeResultEvent event = new ProbeResultEvent(this, result, false);

        when(cacheManager.getCache("probe-results")).thenReturn(probeResultsCache);
        when(cacheManager.getCache("target-status")).thenReturn(targetStatusCache);

        // When
        cacheListener.handleProbeResultEvent(event);

        // Then
        verify(probeResultsCache).evict("target-123");
        verify(targetStatusCache).evict("target-123");
    }

    @Test
    void elasticsearchListener_handlesFailureGracefully() {
        // Given
        ProbeResult result = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                42L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        ProbeResultEvent event = new ProbeResultEvent(this, result, false);

        doThrow(new RuntimeException("ES connection failed")).when(probeRepository).save(any());

        // When - should not throw
        elasticsearchListener.handleProbeResultEvent(event);

        // Then
        verify(probeRepository).save(result);
    }

    @Test
    void cacheListener_handlesFailureGracefully() {
        // Given
        ProbeResult result = new ProbeResult(
                Instant.now(),
                "target-123",
                "example.com",
                42L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        ProbeResultEvent event = new ProbeResultEvent(this, result, false);

        when(cacheManager.getCache("probe-results")).thenReturn(probeResultsCache);

        doThrow(new RuntimeException("Cache eviction failed")).when(probeResultsCache).evict(any());

        // When - should not throw
        cacheListener.handleProbeResultEvent(event);

        // Then - verify it attempted to evict
        verify(probeResultsCache).evict("target-123");
    }
}
