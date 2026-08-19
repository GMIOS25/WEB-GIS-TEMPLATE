package com.website.gis.features.agriculture.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgricultureUnitCreateRequest {

    @NotBlank(message = "Tên đơn vị nông nghiệp không được để trống")
    @Size(max = 255, message = "Tên đơn vị không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 100, message = "Loại đơn vị không được vượt quá 100 ký tự")
    private String unitType;

    private String description;

    @NotBlank(message = "Mã xã/phường không được để trống")
    private String wardCode;

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private BigDecimal longitude;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    private String imageUrl;
}
