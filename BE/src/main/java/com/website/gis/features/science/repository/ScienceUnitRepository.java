package com.website.gis.features.science.repository;

import com.website.gis.features.science.entity.ScienceUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScienceUnitRepository extends JpaRepository<ScienceUnit, Integer> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"ward"})
    List<ScienceUnit> findAll();

    @EntityGraph(attributePaths = {"ward"})
    Page<ScienceUnit> findByWardCode(String wardCode, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Page<ScienceUnit> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Optional<ScienceUnit> findById(Integer id);

    @Query(value = "SELECT id FROM science_units WHERE ST_DWithin(CAST(geom AS geography), ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)", nativeQuery = true)
    List<Integer> findNearbyIds(@Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radiusMeters") double radiusMeters);

    @EntityGraph(attributePaths = {"ward"})
    List<ScienceUnit> findByIdIn(List<Integer> ids);
}
