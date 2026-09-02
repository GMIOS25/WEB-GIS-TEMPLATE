package com.website.gis.features.agriculture.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Modular JPA configuration for the Agriculture feature.
 * Entities and repositories are only scanned and registered when features.agriculture.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "features.agriculture.enabled", havingValue = "true")
@EntityScan(basePackages = "com.website.gis.features.agriculture.entity")
@EnableJpaRepositories(basePackages = "com.website.gis.features.agriculture.repository")
public class AgricultureFeatureConfig {
}
