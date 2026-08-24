package com.website.gis.core.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

/**
 * Tiện ích tập trung để khởi tạo hình học Point trong không gian WGS84 (SRID 4326).
 * Chuẩn hóa thứ tự tham số (latitude, longitude) trên toàn bộ hệ thống.
 */
public final class GisPointUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GisPointUtils() {
        // Utility class
    }

    /**
     * Tạo Point từ Vĩ độ (Latitude) và Kinh độ (Longitude).
     *
     * @param latitude  Vĩ độ (-90 đến 90, trục Y)
     * @param longitude Kinh độ (-180 đến 180, trục X)
     * @return Point WGS84 hoặc null nếu một trong hai giá trị null
     */
    public static Point createPoint(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
    }

    /**
     * Tạo Point từ double (Latitude, Longitude).
     */
    public static Point createPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
