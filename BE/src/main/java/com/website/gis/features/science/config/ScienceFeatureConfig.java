package com.website.gis.features.science.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Modular JPA configuration for the Science & Technology feature.
 * Entities and repositories are only scanned and registered when features.science.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "features.science.enabled", havingValue = "true")
@EntityScan(basePackages = "com.website.gis.features.science.entity")
@EnableJpaRepositories(basePackages = "com.website.gis.features.science.repository")
public class ScienceFeatureConfig {
}
