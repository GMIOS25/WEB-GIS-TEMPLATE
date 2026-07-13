package com.website.gis.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.website.gis.core.entity.GisWard;

import java.util.List;
import java.util.Optional;

public interface GisWardRepository extends JpaRepository<GisWard, Integer> {

    Optional<GisWard> findByWardCode(String wardCode);

    // Giới hạn số chữ số thập phân (6 số ~ sai số 0.1m) để giảm dung lượng JSON, và
    // ST_SimplifyPreserveTopology để giảm SỐ ĐỈNH của polygon — đây mới là yếu tố
    // quyết định tốc độ Canvas renderer vẽ lại mỗi khi zoom/pan (Leaflet Canvas vẽ
    // lại toàn bộ canvas ở mỗi zoomend / khi pan ra ngoài vùng đệm, nên polygon
    // càng
    // nhiều đỉnh càng khựng). Tolerance 0.00003 độ (~3m ở vĩ độ Gia Lai) đủ nhỏ để
    // không làm lệch ranh giới khi nhìn ở mức zoom thông thường, nhưng cắt bớt rất
    // nhiều điểm dư thừa.
    @Query(value = "SELECT ST_AsGeoJSON(ST_SimplifyPreserveTopology(geom, 0.00003), 6) FROM gis_wards WHERE ward_code = :wardCode", nativeQuery = true)
    Optional<String> findGeoJsonByWardCode(@Param("wardCode") String wardCode);

    @Query(value = "SELECT gw.ward_code, w.name, w.full_name, gw.area_km2, " +
            "ST_AsGeoJSON(ST_SimplifyPreserveTopology(gw.geom, 0.00003), 6) " +
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