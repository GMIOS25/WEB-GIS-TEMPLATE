-- View tạo FeatureCollection cho toàn bộ 135 xã/phường tỉnh Gia Lai
-- PostGIS tự biên soạn cây JSON trực tiếp bằng mã C tối ưu, không tốn heap Java
CREATE OR REPLACE VIEW v_wards_geojson AS
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'type', 'Feature',
                'geometry', ST_AsGeoJSON(ST_SimplifyPreserveTopology(gw.geom, 0.00003), 6)::jsonb,
                'properties', jsonb_build_object(
                    'code', gw.ward_code,
                    'name', w.name,
                    'fullName', w.full_name,
                    'areaKm2', gw.area_km2
                )
            )
        ),
        '[]'::jsonb
    )
)::text AS geojson
FROM gis_wards gw
JOIN wards w ON gw.ward_code = w.code
WHERE gw.geom IS NOT NULL;

-- View ranh giới toàn tỉnh
CREATE OR REPLACE VIEW v_province_geojson AS
SELECT ST_AsGeoJSON(ST_SimplifyPreserveTopology(geom, 0.0005), 6) AS geojson
FROM gis_provinces
LIMIT 1;
