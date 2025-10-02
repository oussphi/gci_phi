package com.project.auditing;

import com.project.repository.RequestLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ControllerLoggingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Test
    @WithMockUser(username = "admin@phi.ma", roles = {"ADMIN"})
    void hitting_admin_endpoint_should_create_request_log() throws Exception {
        mockMvc.perform(get("/admin/test-simple"))
                .andExpect(status().isOk());

        assertThat(requestLogRepository.count()).isGreaterThanOrEqualTo(1);
    }
}


