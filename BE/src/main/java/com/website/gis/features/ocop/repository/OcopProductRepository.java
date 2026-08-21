package com.website.gis.features.ocop.repository;

import com.website.gis.features.ocop.entity.OcopProduct;
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
public interface OcopProductRepository extends JpaRepository<OcopProduct, Integer> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"ward"})
    List<OcopProduct> findAll();

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"ward"})
    Page<OcopProduct> findAll(@NonNull Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"ward"})
    Optional<OcopProduct> findById(@NonNull Integer id);

    @EntityGraph(attributePaths = {"ward"})
    Page<OcopProduct> findByWardCode(String wardCode, Pageable pageable);

    @EntityGraph(attributePaths = {"ward"})
    Page<OcopProduct> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"ward"})
    Page<OcopProduct> findByWardCodeAndNameContainingIgnoreCase(String wardCode, String name, Pageable pageable);

    @Query(value = "SELECT * FROM ocop_products WHERE ST_DWithin(CAST(geom AS geography), ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)", nativeQuery = true)
    List<OcopProduct> findNearby(@Param("lat") double lat,
                                 @Param("lng") double lng,
                                 @Param("radiusMeters") double radiusMeters);
}
