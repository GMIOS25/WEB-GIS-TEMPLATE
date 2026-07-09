package com.website.gis.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.website.gis.entity.Ward;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByNameContainingIgnoreCase(String name);
}
