package com.carrierfraud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAuthenticationEntryPointTest {

    @Test
    void commence_writes401WithProblemJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/cases");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());

        entryPoint.commence(request, response, new BadCredentialsException("no creds"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("Authentication required");
        assertThat(response.getContentAsString()).contains("/api/v1/cases");
    }
}