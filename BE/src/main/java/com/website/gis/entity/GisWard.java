package com.website.gis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import java.math.BigDecimal;

@Entity
@Table(name = "gis_wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GisWard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", nullable = false)
    private Ward ward;

    @Column(name = "gis_server_id", length = 50)
    private String gisServerId;

    @Column(name = "area_km2", precision = 12, scale = 5)
    private BigDecimal areaKm2;

    @Column(name = "bbox", columnDefinition = "geometry(Geometry, 4326)")
    private Geometry bbox;

    @Column(name = "geom", columnDefinition = "geometry(Geometry, 4326)")
    private Geometry geom;
}
