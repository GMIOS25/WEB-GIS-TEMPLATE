package com.website.gis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Modular schema strategy (Flyway) - xem docs/en/ARCHITECTURE SPECIFICATION.md
 * muc 5.
 *
 * Core migrations (db/migration/core: V1->V4) luon chay. Cac module theo tinh nang
 * (db/migration/ocop: V5_1.x, db/migration/science: V5_2.x, db/migration/agriculture: V5_3.x)
 * duoc dynamically nap vao scan path khi co feature flag tuong ung duoc bat (= true).
 *
 * Cac entity (OcopProduct, ScienceUnit, AgricultureUnit) va cac script migration
 * tuong ung da duoc trien khai san sang trong codebase.
 */
@Configuration
public class DynamicFlywayConfig {

    @Value("${features.science.enabled:false}")
    private boolean scienceEnabled;

    @Value("${features.ocop.enabled:false}")
    private boolean ocopEnabled;

    @Value("${features.agriculture.enabled:false}")
    private boolean agricultureEnabled;

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            List<String> locations = new ArrayList<>();
            // Core migrations luon phai chay
            locations.add("classpath:db/migration/core");

            // Chi them module theo feature flag NEU thu muc migration tuong ung da ton tai
            if (scienceEnabled) {
                locations.add("classpath:db/migration/science");
            }
            if (ocopEnabled) {
                locations.add("classpath:db/migration/ocop");
            }
            if (agricultureEnabled) {
                locations.add("classpath:db/migration/agriculture");
            }

            configuration.locations(locations.toArray(new String[0]));
            configuration.outOfOrder(true);
        };
    }
}

