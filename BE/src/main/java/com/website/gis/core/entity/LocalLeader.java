package com.website.gis.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "local_leaders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalLeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code")
    private Ward ward;
}
