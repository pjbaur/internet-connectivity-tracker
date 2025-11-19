package me.paulbaur.ict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsIT {

    private final MockMvc mockMvc;

    @Autowired
    OpenApiDocsIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void openApiJson_containsPaths_and_expectedEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(body);

        // top-level 'paths' must exist
        JsonNode paths = root.path("paths");
        assertTrue(!paths.isMissingNode() && paths.isObject(), "OpenAPI JSON must contain 'paths' object");

        // ensure /api/status appears as a documented path
        assertTrue(paths.has("/api/status"), "Expected '/api/status' to be present in OpenAPI paths");

        // history-like endpoints: accept any of these to be present (some apps use /api/probe-results or /api/probe-results/latest)
        List<String> historyCandidates = List.of("/api/history", "/api/probe-results", "/api/probe-results/latest");
        boolean foundHistory = historyCandidates.stream().anyMatch(paths::has);
        assertTrue(foundHistory, "Expected at least one history-like path to be present: " + historyCandidates);

        // ensure there is at least one tag defined
        JsonNode tags = root.path("tags");
        assertTrue(tags.isArray() && tags.size() > 0, "OpenAPI JSON must contain at least one tag");
    }
}
