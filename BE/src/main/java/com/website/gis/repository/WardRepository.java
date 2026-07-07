package com.website.gis.repository;

import com.website.gis.Entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByNameContainingIgnoreCase(String name);
}
