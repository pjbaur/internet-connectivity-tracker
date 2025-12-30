package me.paulbaur.ict.analytics.service;

import me.paulbaur.ict.analytics.domain.LatencyMetrics;
import me.paulbaur.ict.analytics.domain.StateChange;
import me.paulbaur.ict.analytics.domain.TimeSeriesDataPoint;
import me.paulbaur.ict.analytics.domain.UptimeMetrics;
import me.paulbaur.ict.analytics.repository.ElasticsearchAnalyticsRepository;
import me.paulbaur.ict.common.model.ProbeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProbeAnalyticsServiceTest {

    @Mock
    private ElasticsearchAnalyticsRepository analyticsRepository;

    private ProbeAnalyticsService analyticsService;

    private static final String TEST_TARGET_ID = "00000000-0000-0000-0000-000000000000";
    private static final Instant TEST_START = Instant.parse("2025-12-01T00:00:00Z");
    private static final Instant TEST_END = Instant.parse("2025-12-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        analyticsService = new ProbeAnalyticsService(analyticsRepository);
    }

    @Test
    void getUptimeMetrics_whenRepositorySucceeds_returnsMetrics() throws IOException {
        // Arrange
        UptimeMetrics expectedMetrics = new UptimeMetrics(
                TEST_TARGET_ID,
                TEST_START,
                TEST_END,
                1000L,
                950L,
                50L,
                95.0
        );
        when(analyticsRepository.calculateUptime(TEST_TARGET_ID, TEST_START, TEST_END))
                .thenReturn(expectedMetrics);

        // Act
        UptimeMetrics actualMetrics = analyticsService.getUptimeMetrics(TEST_TARGET_ID, TEST_START, TEST_END);

        // Assert
        assertThat(actualMetrics).isEqualTo(expectedMetrics);
        assertThat(actualMetrics.uptimePercentage()).isEqualTo(95.0);
        verify(analyticsRepository).calculateUptime(TEST_TARGET_ID, TEST_START, TEST_END);
    }

    @Test
    void getUptimeMetrics_whenRepositoryThrowsIOException_throwsAnalyticsException() throws IOException {
        // Arrange
        when(analyticsRepository.calculateUptime(TEST_TARGET_ID, TEST_START, TEST_END))
                .thenThrow(new IOException("Elasticsearch connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getUptimeMetrics(TEST_TARGET_ID, TEST_START, TEST_END))
                .isInstanceOf(ProbeAnalyticsService.AnalyticsException.class)
                .hasMessageContaining("Failed to calculate uptime metrics for target: " + TEST_TARGET_ID)
                .hasCauseInstanceOf(IOException.class);

        verify(analyticsRepository).calculateUptime(TEST_TARGET_ID, TEST_START, TEST_END);
    }

    @Test
    void getLatencyMetrics_whenRepositorySucceeds_returnsMetrics() throws IOException {
        // Arrange
        LatencyMetrics expectedMetrics = new LatencyMetrics(
                TEST_TARGET_ID,
                TEST_START,
                TEST_END,
                42.5,
                10.0,
                150.0,
                950L
        );
        when(analyticsRepository.calculateLatency(TEST_TARGET_ID, TEST_START, TEST_END))
                .thenReturn(expectedMetrics);

        // Act
        LatencyMetrics actualMetrics = analyticsService.getLatencyMetrics(TEST_TARGET_ID, TEST_START, TEST_END);

        // Assert
        assertThat(actualMetrics).isEqualTo(expectedMetrics);
        assertThat(actualMetrics.averageLatencyMs()).isEqualTo(42.5);
        assertThat(actualMetrics.minLatencyMs()).isEqualTo(10.0);
        assertThat(actualMetrics.maxLatencyMs()).isEqualTo(150.0);
        verify(analyticsRepository).calculateLatency(TEST_TARGET_ID, TEST_START, TEST_END);
    }

    @Test
    void getLatencyMetrics_whenRepositoryThrowsIOException_throwsAnalyticsException() throws IOException {
        // Arrange
        when(analyticsRepository.calculateLatency(TEST_TARGET_ID, TEST_START, TEST_END))
                .thenThrow(new IOException("Elasticsearch connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getLatencyMetrics(TEST_TARGET_ID, TEST_START, TEST_END))
                .isInstanceOf(ProbeAnalyticsService.AnalyticsException.class)
                .hasMessageContaining("Failed to calculate latency metrics for target: " + TEST_TARGET_ID)
                .hasCauseInstanceOf(IOException.class);

        verify(analyticsRepository).calculateLatency(TEST_TARGET_ID, TEST_START, TEST_END);
    }

    @Test
    void getStateChanges_whenRepositorySucceeds_returnsStateChanges() throws IOException {
        // Arrange
        List<StateChange> expectedChanges = List.of(
                new StateChange(
                        TEST_TARGET_ID,
                        Instant.parse("2025-12-15T10:00:00Z"),
                        ProbeStatus.UP,
                        ProbeStatus.DOWN,
                        "Connection timeout"
                ),
                new StateChange(
                        TEST_TARGET_ID,
                        Instant.parse("2025-12-15T10:05:00Z"),
                        ProbeStatus.DOWN,
                        ProbeStatus.UP,
                        null
                )
        );
        when(analyticsRepository.findStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100))
                .thenReturn(expectedChanges);

        // Act
        List<StateChange> actualChanges = analyticsService.getStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100);

        // Assert
        assertThat(actualChanges).hasSize(2);
        assertThat(actualChanges).isEqualTo(expectedChanges);
        verify(analyticsRepository).findStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100);
    }

    @Test
    void getStateChanges_whenRepositoryThrowsIOException_throwsAnalyticsException() throws IOException {
        // Arrange
        when(analyticsRepository.findStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100))
                .thenThrow(new IOException("Elasticsearch connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100))
                .isInstanceOf(ProbeAnalyticsService.AnalyticsException.class)
                .hasMessageContaining("Failed to find state changes for target: " + TEST_TARGET_ID)
                .hasCauseInstanceOf(IOException.class);

        verify(analyticsRepository).findStateChanges(TEST_TARGET_ID, TEST_START, TEST_END, 100);
    }

    @Test
    void getTimeSeries_whenRepositorySucceeds_returnsDataPoints() throws IOException {
        // Arrange
        List<TimeSeriesDataPoint> expectedDataPoints = List.of(
                new TimeSeriesDataPoint(
                        Instant.parse("2025-12-15T00:00:00Z"),
                        45.0,
                        60L,
                        58L,
                        96.67
                ),
                new TimeSeriesDataPoint(
                        Instant.parse("2025-12-15T01:00:00Z"),
                        42.0,
                        60L,
                        60L,
                        100.0
                )
        );
        when(analyticsRepository.calculateTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h"))
                .thenReturn(expectedDataPoints);

        // Act
        List<TimeSeriesDataPoint> actualDataPoints = analyticsService.getTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h");

        // Assert
        assertThat(actualDataPoints).hasSize(2);
        assertThat(actualDataPoints).isEqualTo(expectedDataPoints);
        assertThat(actualDataPoints.get(0).uptimePercentage()).isEqualTo(96.67);
        assertThat(actualDataPoints.get(1).uptimePercentage()).isEqualTo(100.0);
        verify(analyticsRepository).calculateTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h");
    }

    @Test
    void getTimeSeries_whenRepositoryThrowsIOException_throwsAnalyticsException() throws IOException {
        // Arrange
        when(analyticsRepository.calculateTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h"))
                .thenThrow(new IOException("Elasticsearch connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h"))
                .isInstanceOf(ProbeAnalyticsService.AnalyticsException.class)
                .hasMessageContaining("Failed to calculate time series for target: " + TEST_TARGET_ID)
                .hasCauseInstanceOf(IOException.class);

        verify(analyticsRepository).calculateTimeSeries(TEST_TARGET_ID, TEST_START, TEST_END, "1h");
    }
}
