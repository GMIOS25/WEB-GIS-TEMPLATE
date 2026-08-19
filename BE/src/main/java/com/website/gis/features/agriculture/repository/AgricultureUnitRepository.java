package com.website.gis.features.agriculture.repository;

import com.website.gis.features.agriculture.entity.AgricultureUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgricultureUnitRepository extends JpaRepository<AgricultureUnit, Integer> {

    @EntityGraph(attributePaths = {"ward"})
    Page<AgricultureUnit> findByWardCode(String wardCode, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Page<AgricultureUnit> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Optional<AgricultureUnit> findById(Integer id);
}
