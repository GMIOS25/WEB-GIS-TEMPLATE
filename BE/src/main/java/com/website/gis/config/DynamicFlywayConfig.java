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
 * Core migrations (auth, admin units, GIS) luon chay. Cac module theo tinh nang
 * (science/ocop/agriculture) chi duoc bat khi co feature flag tuong ung VA
 * thu muc db/migration/<feature> da ton tai voi it nhat 1 file V*.sql.
 *
 * LUU Y: cac thu muc science/ocop/agriculture hien CHUA duoc tao vi entity
 * tuong ung (Science, Ocop, Agriculture) chua duoc trien khai trong code (Giai doan 2
 * - roadmap).
 * Khi implement entity nao, hay tao thu muc db/migration/<feature> voi file
 * V*.sql
 * tuong ung TRUOC KHI bat feature flag do, neu khong Flyway se bao loi
 * "Unable to resolve location" khi khoi dong.
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
        };
    }
}
