package com.carrierfraud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudSentinelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FraudSentinel API")
                        .description("Carrier fraud detection platform for freight exchange marketplaces. " +
                                "Provides RBAC-based alert workflow, real-time client-to-staff communication, " +
                                "document management with categorization, and 72h SLA tracking.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jess Borges")
                                .url("https://github.com/Borgesjesk"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Login, logout, refresh token, session bootstrap"),
                        new Tag().name("Alerts").description("Fraud alert workflow: accept, investigate, resolve, escalate, transfer"),
                        new Tag().name("Complaints").description("Client-submitted complaints with document upload"),
                        new Tag().name("Comments").description("Client-staff messaging thread per alert"),
                        new Tag().name("Notes").description("Staff-only internal notes with role-gated access"),
                        new Tag().name("Analytics").description("Unread counts, stale alerts, dashboard metrics")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("FS_SESSION")
                                        .description("HttpOnly JWT session cookie. Set automatically by /auth/login.")));
    }
}