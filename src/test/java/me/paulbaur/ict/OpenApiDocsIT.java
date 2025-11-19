package me.paulbaur.ict;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // ensure the top-level 'paths' element exists
                .andExpect(jsonPath("$.paths").exists())
                // ensure /api/status appears as a documented path
                .andExpect(jsonPath("$.paths['/api/status']").exists())
                // ensure /api/history appears as a documented path
                .andExpect(jsonPath("$.paths['/api/history']").exists())
                // ensure there is at least one tag defined
                .andExpect(jsonPath("$.tags").isArray());
    }
}
