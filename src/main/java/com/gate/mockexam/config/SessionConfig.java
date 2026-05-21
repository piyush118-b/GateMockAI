package com.gate.mockexam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // Explicitly set cookie name to SESSION
        serializer.setCookieName("SESSION");
        // Set session cookie Max-Age to 30 days in seconds (30 * 24 * 60 * 60)
        // This ensures the cookie persists on the client side even after browser restarts
        serializer.setCookieMaxAge(2592000);
        serializer.setCookiePath("/");
        serializer.setSameSite("Lax");
        return serializer;
    }
}
