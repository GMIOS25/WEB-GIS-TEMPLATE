package com.website.gis.config;

import com.website.gis.core.mapper.UserMapper;
import com.website.gis.core.mapper.WardMapper;
import com.website.gis.features.agriculture.mapper.AgricultureUnitMapper;
import com.website.gis.features.ocop.mapper.OcopProductMapper;
import com.website.gis.features.science.mapper.ScienceUnitMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Shared test configuration providing MapStruct mapper beans for @WebMvcTest slices.
 *
 * Uses MapStruct's dynamic Mappers.getMapper(...) factory at runtime to decouple test
 * compilation from generated *MapperImpl classes, preventing IDE unresolved-symbol errors.
 */
@TestConfiguration
public class TestMapperConfig {

    @Bean
    public UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }

    @Bean
    public WardMapper wardMapper() {
        return Mappers.getMapper(WardMapper.class);
    }

    @Bean
    public AgricultureUnitMapper agricultureUnitMapper() {
        return Mappers.getMapper(AgricultureUnitMapper.class);
    }

    @Bean
    public OcopProductMapper ocopProductMapper() {
        return Mappers.getMapper(OcopProductMapper.class);
    }

    @Bean
    public ScienceUnitMapper scienceUnitMapper() {
        return Mappers.getMapper(ScienceUnitMapper.class);
    }
}
