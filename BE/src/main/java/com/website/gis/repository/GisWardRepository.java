package com.website.gis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.website.gis.entity.GisWard;

import java.util.List;
import java.util.Optional;

public interface GisWardRepository extends JpaRepository<GisWard, Integer> {

    Optional<GisWard> findByWardCode(String wardCode);

    // Giới hạn số chữ số thập phân (6 số ~ sai số 0.1m) để giảm đáng kể dung lượng
    // JSON
    // trả về so với độ chính xác mặc định (mặc định có thể lên tới 15 chữ số).
    @Query(value = "SELECT ST_AsGeoJSON(geom, 6) FROM gis_wards WHERE ward_code = :wardCode", nativeQuery = true)
    Optional<String> findGeoJsonByWardCode(@Param("wardCode") String wardCode);

    @Query(value = "SELECT gw.ward_code, w.name, w.full_name, gw.area_km2, ST_AsGeoJSON(gw.geom, 6) " +
            "FROM gis_wards gw JOIN wards w ON gw.ward_code = w.code", nativeQuery = true)
    List<Object[]> findAllWardsGeoJsonData();

    // Layer tổng quan (toàn tỉnh) hiển thị ở zoom thấp nên có thể giảm bớt độ chi
    // tiết
    // hình học bằng ST_SimplifyPreserveTopology mà không ảnh hưởng cảm nhận trực
    // quan.
    // Điều chỉnh tolerance (đơn vị: độ, ví dụ 0.0005 ~ 50m) tuỳ theo mức độ chi
    // tiết mong muốn.
    @Query(value = "SELECT ST_AsGeoJSON(ST_SimplifyPreserveTopology(geom, 0.0005), 6) FROM gis_provinces LIMIT 1", nativeQuery = true)
    Optional<String> findProvinceGeoJson();
}