package com.website.gis.features.ocop.entity;

import com.website.gis.core.entity.Ward;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

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

    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", nullable = false)
    private Ward ward;

    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point geom;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
