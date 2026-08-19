package com.website.gis.features.agriculture.entity;

import com.website.gis.core.entity.Ward;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "agriculture_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgricultureUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_type", length = 100)
    private String unitType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", referencedColumnName = "code", nullable = false)
    private Ward ward;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
