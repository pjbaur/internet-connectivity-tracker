package me.paulbaur.ict.probe.api;

import me.paulbaur.ict.TestContainersConfig;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
class ProbeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProbeService probeService;

    @Test
    void latest_returnsLatestResult() throws Exception {
        ProbeResult result = new ProbeResult(
                Instant.parse("2025-11-19T12:34:56Z"),
                "00000000-0000-0000-0000-000000000000",
                "example.org",
                23L,
                "cycle-123",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );
        when(probeService.getLatestResult()).thenReturn(Optional.of(result));

        mockMvc.perform(get("/api/probe-results/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value("2025-11-19T12:34:56Z"))
                .andExpect(jsonPath("$.targetId").value("00000000-0000-0000-0000-000000000000"))
                .andExpect(jsonPath("$.targetHost").value("example.org"))
                .andExpect(jsonPath("$.latencyMs").value(23))
                .andExpect(jsonPath("$.probeCycleId").value("cycle-123"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.method").value("TCP"))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()));
    }

    @Test
    void latest_whenMissing_returnsNotFound() throws Exception {
        when(probeService.getLatestResult()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/probe-results/latest"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No probe result available"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void recent_withValidParams_returnsResults() throws Exception {
        String targetId = "11111111-2222-3333-4444-555555555555";
        List<ProbeResult> results = List.of(
                new ProbeResult(Instant.parse("2025-11-19T12:34:56Z"), targetId, "one.example", 10L, "cycle-1", ProbeStatus.UP, ProbeMethod.TCP, null),
                new ProbeResult(Instant.parse("2025-11-19T12:33:56Z"), targetId, "one.example", null, "cycle-1", ProbeStatus.DOWN, ProbeMethod.TCP, "timeout")
        );
        when(probeService.getRecentResultsForTarget(targetId, 2)).thenReturn(results);

        mockMvc.perform(get("/api/probe/targets/{targetId}/recent", targetId).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].timestamp").value("2025-11-19T12:34:56Z"))
                .andExpect(jsonPath("$[0].probeCycleId").value("cycle-1"))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[1].status").value("DOWN"))
                .andExpect(jsonPath("$[1].probeCycleId").value("cycle-1"))
                .andExpect(jsonPath("$[1].errorMessage").value("timeout"));

        verify(probeService).getRecentResultsForTarget(targetId, 2);
    }

    @Test
    void recent_withInvalidLimit_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/probe/targets/{targetId}/recent", "target-123").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("limit must be between 1 and 5000"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void history_withMissingEnd_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("targetId", "target-123")
                        .param("start", "2025-11-19T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("start and end must both be provided together"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void history_withInvalidTimestamp_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("targetId", "target-123")
                        .param("limit", "5")
                        .param("start", "not-a-timestamp")
                        .param("end", "2025-11-19T12:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("start must be an ISO-8601 timestamp"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void history_withValidRange_returnsResults() throws Exception {
        String targetId = "11111111-2222-3333-4444-555555555555";
        Instant start = Instant.parse("2025-11-19T10:00:00Z");
        Instant end = Instant.parse("2025-11-19T12:00:00Z");
        List<ProbeResult> results = List.of(
                new ProbeResult(Instant.parse("2025-11-19T11:10:00Z"), targetId, "range.example", 42L, "cycle-5", ProbeStatus.UP, ProbeMethod.TCP, null)
        );
        when(probeService.getHistoryForTarget(targetId, 5, start, end)).thenReturn(results);

        mockMvc.perform(get("/api/history")
                        .param("targetId", targetId)
                        .param("limit", "5")
                        .param("start", "2025-11-19T10:00:00Z")
                        .param("end", "2025-11-19T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].timestamp").value("2025-11-19T11:10:00Z"))
                .andExpect(jsonPath("$[0].targetHost").value("range.example"))
                .andExpect(jsonPath("$[0].probeCycleId").value("cycle-5"))
                .andExpect(jsonPath("$[0].status").value("UP"));

        verify(probeService).getHistoryForTarget(eq(targetId), eq(5), eq(start), eq(end));
    }
}
