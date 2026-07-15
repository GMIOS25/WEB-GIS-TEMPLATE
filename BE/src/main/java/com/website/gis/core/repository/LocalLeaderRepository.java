package com.website.gis.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.website.gis.core.entity.LocalLeader;

import java.util.List;

public interface LocalLeaderRepository
        extends JpaRepository<LocalLeader, Integer>, JpaSpecificationExecutor<LocalLeader> {
    List<LocalLeader> findByWardCode(String wardCode);
}
