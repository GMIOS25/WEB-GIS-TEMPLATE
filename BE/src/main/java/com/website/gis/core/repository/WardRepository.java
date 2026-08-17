package com.website.gis.core.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.website.gis.core.entity.Ward;

import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, String> {

    // @EntityGraph(attributePaths = "province"): nạp sẵn quan hệ province (LAZY -
    // xem Ward.java) trong CÙNG 1 câu query thay vì để Hibernate tự lazy-load khi
    // WardMapper đọc ward.getProvince().getFullName(). Bắt buộc phải có kể từ khi
    // spring.jpa.open-in-view=false (xem application.properties.example): trước
    // đây OSIV mặc định true của Spring Boot "che" được việc thiếu fetch tường
    // minh này (Hibernate session còn mở suốt request nên lazy-load ngoài
    // transaction vẫn chạy được) - nhưng đó là anti-pattern (Spring Boot khuyến
    // cáo tắt OSIV) và ẩn chứa rủi ro N+1 query. Override lại 2 method kế thừa từ
    // JpaRepository (findAll/findById) để áp @EntityGraph, vì bản mặc định của
    // chúng không có.
    @Override
    @NonNull
    @EntityGraph(attributePaths = "province")
    List<Ward> findAll();

    @Override
    @NonNull
    @EntityGraph(attributePaths = "province")
    Optional<Ward> findById(@NonNull String code);

    // Trước đây chỉ search theo `name` (tên ngắn, vd. "Ia Kring"), bỏ sót
    // `full_name` (tên đầy đủ, vd. "Phường Ia Kring") - người dùng gõ nguyên tên
    // đầy đủ có thể không ra kết quả dù tên ngắn có khớp. Gộp cả 2 cột bằng OR.
    @EntityGraph(attributePaths = "province")
    List<Ward> findByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(String name, String fullName);
}
