package com.website.gis.features.agriculture.repository;

import com.website.gis.features.agriculture.entity.AgricultureUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface AgricultureUnitRepository extends JpaRepository<AgricultureUnit, Integer> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"ward"})
    List<AgricultureUnit> findAll();

    @EntityGraph(attributePaths = {"ward"})
    Page<AgricultureUnit> findByWardCode(String wardCode, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Page<AgricultureUnit> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Optional<AgricultureUnit> findById(Integer id);

    @Query(value = "SELECT id FROM agriculture_units WHERE ST_DWithin(CAST(geom AS geography), ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)", nativeQuery = true)
    List<Integer> findNearbyIds(@Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radiusMeters") double radiusMeters);

    @Query(value = "SELECT geojson FROM v_agriculture_geojson", nativeQuery = true)
    Optional<String> findAgricultureFeatureCollection();

    @EntityGraph(attributePaths = {"ward"})
    List<AgricultureUnit> findByIdIn(List<Integer> ids);
}
