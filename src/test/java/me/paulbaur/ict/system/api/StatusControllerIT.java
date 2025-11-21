package me.paulbaur.ict.system.api;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class StatusControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProbeService probeService;

    @MockBean
    private TargetService targetService;

    @Test
    void getStatus_withMixedResults_computesSnapshot() throws Exception {
        UUID downId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID disabledId = UUID.fromString("00000000-0000-0000-0000-000000000012");

        Target downTarget = new Target(downId, "Down Target", "down.example", 80, true);
        Target unknownTarget = new Target(unknownId, "Unknown Target", "unknown.example", 81, true);
        Target disabledTarget = new Target(disabledId, "Disabled Target", "disabled.example", 82, false);
        when(targetService.findAll()).thenReturn(List.of(downTarget, unknownTarget, disabledTarget));

        ProbeResult downResult = new ProbeResult(
                Instant.parse("2025-11-19T12:00:00Z"),
                downId.toString(),
                downTarget.getHost(),
                125L,
                ProbeStatus.DOWN,
                ProbeMethod.TCP,
                "connection refused"
        );
        when(probeService.getRecentResultsForTarget(downId.toString(), 1)).thenReturn(List.of(downResult));
        when(probeService.getRecentResultsForTarget(unknownId.toString(), 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.anyDown").value(true))
                .andExpect(jsonPath("$.totalTargets").value(2))
                .andExpect(jsonPath("$.targetsDown").value(1))
                .andExpect(jsonPath("$.unknownTargets").value(1))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(probeService).getRecentResultsForTarget(downId.toString(), 1);
        verify(probeService).getRecentResultsForTarget(unknownId.toString(), 1);
        verify(probeService, never()).getRecentResultsForTarget(disabledId.toString(), 1);
    }
}
