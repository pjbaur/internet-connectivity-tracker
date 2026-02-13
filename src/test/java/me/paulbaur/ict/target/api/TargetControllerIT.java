package me.paulbaur.ict.target.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.paulbaur.ict.TestContainersConfig;
import me.paulbaur.ict.target.api.dto.TargetRequestDto;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
class TargetControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TargetService targetService;

    @Test
    void listTargets_returnsMappedDtos() throws Exception {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Target target = new Target(id, "Example", "example.org", 443, true);
        when(targetService.findAll()).thenReturn(List.of(target));

        mockMvc.perform(get("/api/targets"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].label").value("Example"))
                .andExpect(jsonPath("$[0].host").value("example.org"))
                .andExpect(jsonPath("$[0].port").value(443))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void createTarget_validRequest_returnsCreatedTarget() throws Exception {
        TargetRequestDto request = new TargetRequestDto("New Target", "new.example.org", 8443);
        UUID createdId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Target createdTarget = new Target(createdId, request.label(), request.host(), request.port(), true);
        when(targetService.create(any(Target.class))).thenReturn(createdTarget);

        mockMvc.perform(post("/api/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdId.toString()))
                .andExpect(jsonPath("$.label").value("New Target"))
                .andExpect(jsonPath("$.host").value("new.example.org"))
                .andExpect(jsonPath("$.port").value(8443))
                .andExpect(jsonPath("$.enabled").value(true));

        ArgumentCaptor<Target> captor = ArgumentCaptor.forClass(Target.class);
        verify(targetService).create(captor.capture());
        Target saved = captor.getValue();
        assertThat(saved.getLabel()).isEqualTo("New Target");
        assertThat(saved.getHost()).isEqualTo("new.example.org");
        assertThat(saved.getPort()).isEqualTo(8443);
    }

    @Test
    void createTarget_missingLabel_returnsBadRequest() throws Exception {
        String body = """
                {
                  "host": "missing-label.example",
                  "port": 80
                }
                """;

        mockMvc.perform(post("/api/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("label is required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(targetService, never()).create(any(Target.class));
    }

    @Test
    void deleteTarget_existingTarget_returnsNoContent() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(targetService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/targets/{id}", id))
                .andExpect(status().isNoContent());

        verify(targetService).delete(id);
    }

    @Test
    void deleteTarget_invalidUuid_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/targets/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("id must be a valid UUID"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(targetService, never()).delete(any());
    }

    @Test
    void deleteTarget_missingTarget_returnsNotFound() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000009");
        when(targetService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/targets/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Target not found for id " + id))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
