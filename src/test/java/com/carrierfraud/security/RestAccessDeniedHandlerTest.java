package com.carrierfraud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAccessDeniedHandlerTest {

    @Test
    void handle_writes403WithProblemJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/actuator/shutdown");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(new ObjectMapper());

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("Access denied");
        assertThat(response.getContentAsString()).contains("/actuator/shutdown");
    }
}