package com.carrierfraud.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CookiePropertiesTest {

    private static final String NAME = "app.cookie.name=FS_SESSION";
    private static final String SECURE = "app.cookie.secure=false";
    private static final String SAME_SITE = "app.cookie.same-site=Strict";
    private static final String MAX_AGE = "app.cookie.max-age-seconds=3600";
    private static final String PATH = "app.cookie.path=/";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsAllDefaultsCorrectly() {
        runner.withPropertyValues(NAME, SECURE, SAME_SITE, MAX_AGE, PATH).run(ctx -> {
            CookieProperties props = ctx.getBean(CookieProperties.class);
            assertThat(props.name()).isEqualTo("FS_SESSION");
            assertThat(props.secure()).isFalse();
            assertThat(props.sameSite()).isEqualTo("Strict");
            assertThat(props.maxAgeSeconds()).isEqualTo(3600L);
            assertThat(props.path()).isEqualTo("/");
        });
    }

    @Test
    void rejectsInvalidSameSitePattern() {
        runner.withPropertyValues(NAME, SECURE, "app.cookie.same-site=Invalid", MAX_AGE, PATH)
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void rejectsNonPositiveMaxAgeSeconds() {
        runner.withPropertyValues(NAME, SECURE, SAME_SITE, "app.cookie.max-age-seconds=-1", PATH)
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @EnableConfigurationProperties(CookieProperties.class)
    static class TestConfig {
    }
}
