package com.website.gis.features.ocop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Modular JPA configuration for the OCOP feature.
 * Entities and repositories are only scanned and registered when features.ocop.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
@EntityScan(basePackages = "com.website.gis.features.ocop.entity")
@EnableJpaRepositories(basePackages = "com.website.gis.features.ocop.repository")
public class OcopFeatureConfig {
}
