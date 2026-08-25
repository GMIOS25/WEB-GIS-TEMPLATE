package com.website.gis.features.ocop.entity;

import com.website.gis.core.entity.Ward;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Entity
@Table(name = "ocop_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcopProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "product_types", columnDefinition = "text[]")
    private List<String> productTypes;

    @Column(name = "star_rating")
    private Integer starRating;

    @Column(name = "contact_phone", length = 13)
    private String contactPhone;

    @Column(name = "location_address", columnDefinition = "TEXT")
    private String locationAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", nullable = false)
    private Ward ward;

    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point geom;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
