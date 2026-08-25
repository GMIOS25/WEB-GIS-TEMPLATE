package com.website.gis.features.ocop.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcopProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm OCOP không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không vượt quá 255 ký tự")
    private String name;

    private List<String> productTypes;

    @Min(value = 1, message = "Số sao OCOP phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Số sao OCOP phải từ 1 đến 5 sao")
    private Integer starRating;

    @Size(max = 13, message = "Số điện thoại liên hệ không vượt quá 13 ký tự")
    private String contactPhone;

    private String locationAddress;

    @NotBlank(message = "Mã xã/phường không được để trống")
    @Size(max = 20, message = "Mã xã/phường không vượt quá 20 ký tự")
    private String wardCode;

    @NotNull(message = "Vĩ độ (latitude) là bắt buộc")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90.0 đến 90.0")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90.0 đến 90.0")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ (longitude) là bắt buộc")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180.0 đến 180.0")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180.0 đến 180.0")
    private BigDecimal longitude;

    @Size(max = 500, message = "Đường dẫn ảnh không vượt quá 500 ký tự")
    private String imageUrl;
}
