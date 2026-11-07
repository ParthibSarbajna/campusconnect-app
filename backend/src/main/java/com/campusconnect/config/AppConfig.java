package com.campusconnect.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    /** Injectable clock so time-dependent code can be tested deterministically. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
