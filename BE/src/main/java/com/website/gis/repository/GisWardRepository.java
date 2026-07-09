package com.website.gis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.website.gis.entity.GisWard;

import java.util.List;
import java.util.Optional;

public interface GisWardRepository extends JpaRepository<GisWard, Integer> {
    
    Optional<GisWard> findByWardCode(String wardCode);

    @Query(value = "SELECT ST_AsGeoJSON(geom) FROM gis_wards WHERE ward_code = :wardCode", nativeQuery = true)
    Optional<String> findGeoJsonByWardCode(@Param("wardCode") String wardCode);

    @Query(value = "SELECT gw.ward_code, w.name, w.full_name, gw.area_km2, ST_AsGeoJSON(gw.geom) " +
                   "FROM gis_wards gw JOIN wards w ON gw.ward_code = w.code", nativeQuery = true)
    List<Object[]> findAllWardsGeoJsonData();

    @Query(value = "SELECT ST_AsGeoJSON(geom) FROM gis_provinces LIMIT 1", nativeQuery = true)
    Optional<String> findProvinceGeoJson();
}
