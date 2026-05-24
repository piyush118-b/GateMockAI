package com.gate.mockexam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * TRACK 6 — Redis session configuration.
 *
 * Active only under the "redis" Spring profile:
 *   --spring.profiles.active=redis
 *
 * Uses @ConditionalOnMissingBean so that an externally-provided
 * RedisConnectionFactory (e.g. from Spring Cloud, test containers, or a
 * custom RedisClusterConfiguration) takes precedence.
 *
 * SessionConfig's CookieSerializer (Max-Age 30 days, SameSite=Lax) applies
 * identically — Redis sessions use the same cookie mechanism.
 *
 * CSRF: SecurityConfig calls csrf.disable(), so HttpSessionCsrfTokenRepository
 * (the default) is never registered; no CSRF changes are needed.
 */
@Configuration
@Profile("redis")
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 2592000 // 30 days — matches cookie Max-Age
)
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Exposes a Lettuce connection factory only when no RedisConnectionFactory
     * bean has already been defined in the application context.
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }
}
