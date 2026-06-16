package com.domriquez.spendwise.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's annotation-driven caching ({@code @Cacheable}, {@code @CacheEvict}).
 *
 * <p>The {@code CacheManager} itself is not declared here: with {@code spring-boot-starter-data-redis}
 * on the classpath and {@code spring.cache.type=redis} configured, Spring Boot auto-configures a
 * {@code RedisCacheManager}. This class only needs to enable the caching infrastructure.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
