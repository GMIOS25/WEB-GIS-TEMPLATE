package com.website.gis.config;

import com.website.gis.core.config.CoreJpaConfig;
import com.website.gis.features.agriculture.config.AgricultureFeatureConfig;
import com.website.gis.features.ocop.config.OcopFeatureConfig;
import com.website.gis.features.science.config.ScienceFeatureConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

class ModularFeatureConfigTest {

    @Test
    @DisplayName("CoreJpaConfig has correct configuration and scans core packages")
    void coreJpaConfig_annotations() {
        assertThat(CoreJpaConfig.class.isAnnotationPresent(Configuration.class)).isTrue();

        EntityScan entityScan = CoreJpaConfig.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactly("com.website.gis.core.entity");

        EnableJpaRepositories repoScan = CoreJpaConfig.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(repoScan).isNotNull();
        assertThat(repoScan.basePackages()).containsExactly("com.website.gis.core.repository");
    }

    @Test
    @DisplayName("OcopFeatureConfig has ConditionalOnProperty, EntityScan, and EnableJpaRepositories")
    void ocopFeatureConfig_annotations() {
        assertThat(OcopFeatureConfig.class.isAnnotationPresent(Configuration.class)).isTrue();

        ConditionalOnProperty cond = OcopFeatureConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(cond).isNotNull();
        assertThat(cond.name()).containsExactly("features.ocop.enabled");
        assertThat(cond.havingValue()).isEqualTo("true");

        EntityScan entityScan = OcopFeatureConfig.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactly("com.website.gis.features.ocop.entity");

        EnableJpaRepositories repoScan = OcopFeatureConfig.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(repoScan).isNotNull();
        assertThat(repoScan.basePackages()).containsExactly("com.website.gis.features.ocop.repository");
    }

    @Test
    @DisplayName("ScienceFeatureConfig has ConditionalOnProperty, EntityScan, and EnableJpaRepositories")
    void scienceFeatureConfig_annotations() {
        assertThat(ScienceFeatureConfig.class.isAnnotationPresent(Configuration.class)).isTrue();

        ConditionalOnProperty cond = ScienceFeatureConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(cond).isNotNull();
        assertThat(cond.name()).containsExactly("features.science.enabled");
        assertThat(cond.havingValue()).isEqualTo("true");

        EntityScan entityScan = ScienceFeatureConfig.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactly("com.website.gis.features.science.entity");

        EnableJpaRepositories repoScan = ScienceFeatureConfig.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(repoScan).isNotNull();
        assertThat(repoScan.basePackages()).containsExactly("com.website.gis.features.science.repository");
    }

    @Test
    @DisplayName("AgricultureFeatureConfig has ConditionalOnProperty, EntityScan, and EnableJpaRepositories")
    void agricultureFeatureConfig_annotations() {
        assertThat(AgricultureFeatureConfig.class.isAnnotationPresent(Configuration.class)).isTrue();

        ConditionalOnProperty cond = AgricultureFeatureConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(cond).isNotNull();
        assertThat(cond.name()).containsExactly("features.agriculture.enabled");
        assertThat(cond.havingValue()).isEqualTo("true");

        EntityScan entityScan = AgricultureFeatureConfig.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactly("com.website.gis.features.agriculture.entity");

        EnableJpaRepositories repoScan = AgricultureFeatureConfig.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(repoScan).isNotNull();
        assertThat(repoScan.basePackages()).containsExactly("com.website.gis.features.agriculture.repository");
    }
}
