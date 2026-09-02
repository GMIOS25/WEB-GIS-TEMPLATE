package com.website.gis.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Core JPA configuration.
 * Always scanned to initialize core entities (Province, Ward, GisProvince, GisWard, LocalLeader, User)
 * and their respective repositories.
 */
@Configuration
@EntityScan(basePackages = "com.website.gis.core.entity")
@EnableJpaRepositories(basePackages = "com.website.gis.core.repository")
public class CoreJpaConfig {
}
