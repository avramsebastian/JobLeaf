package com.seby.jobleaf.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * In-memory caching with Caffeine, used through Spring's cache annotations ({@code @Cacheable}, {@code @CacheEvict}).
 * <p>
 * Kept simple on purpose: the cache lives inside this backend instance (not shared between instances, empty after a
 * restart). Switching to Redis later means replacing this bean; the annotations stay the same.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** The public company list with all jobs ({@code GET /companies/public}). */
    public static final String PUBLIC_COMPANIES = "publicCompanies";

    /** Safety net: even if an eviction is ever missed, data is at most this old. */
    static final Duration TIME_TO_LIVE = Duration.ofMinutes(10);
    static final long MAXIMUM_SIZE = 10;

    @Bean
    public CacheManager caffeineCacheManager() {
        // Cache names given up front: only these caches exist, so a typo in an annotation fails at the first call
        // instead of silently creating a new, never-evicted cache
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(PUBLIC_COMPANIES);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(TIME_TO_LIVE)
                .recordStats());

        cacheManager.setAllowNullValues(false);

        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
