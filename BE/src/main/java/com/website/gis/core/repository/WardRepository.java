package com.website.gis.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.website.gis.core.entity.Ward;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByNameContainingIgnoreCase(String name);
}
