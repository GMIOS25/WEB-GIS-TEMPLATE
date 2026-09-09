package com.website.gis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ALL_WARDS_GEOJSON = "allWardsGeoJson";
    public static final String WARD_GEOJSON = "wardGeoJson";
    public static final String PROVINCE_GEOJSON = "provinceGeoJson";
    public static final String OCOP_GEOJSON = "ocopGeoJson";
    public static final String SCIENCE_GEOJSON = "scienceGeoJson";
    public static final String AGRICULTURE_GEOJSON = "agricultureGeoJson";
    public static final String USER_DETAILS = "userDetails";

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.geojson-ttl:1h}") Duration geoJsonTtl,
            @Value("${app.cache.user-details-ttl:5m}") Duration userDetailsTtl) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache(ALL_WARDS_GEOJSON, 1, geoJsonTtl),
                cache(WARD_GEOJSON, 256, geoJsonTtl),
                cache(PROVINCE_GEOJSON, 1, geoJsonTtl),
                cache(OCOP_GEOJSON, 1, geoJsonTtl),
                cache(SCIENCE_GEOJSON, 1, geoJsonTtl),
                cache(AGRICULTURE_GEOJSON, 1, geoJsonTtl),
                cache(USER_DETAILS, 10_000, userDetailsTtl)));
        return manager;
    }

    private static CaffeineCache cache(String name, long maximumSize, Duration ttl) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build());
    }
}
