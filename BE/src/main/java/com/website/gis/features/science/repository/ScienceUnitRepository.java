package com.website.gis.features.science.repository;

import com.website.gis.features.science.entity.ScienceUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScienceUnitRepository extends JpaRepository<ScienceUnit, Integer> {

    @EntityGraph(attributePaths = {"ward"})
    Page<ScienceUnit> findByWardCode(String wardCode, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Page<ScienceUnit> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"ward"})
    Optional<ScienceUnit> findById(Integer id);
}
