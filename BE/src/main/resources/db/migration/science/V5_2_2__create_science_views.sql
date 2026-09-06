-- View tạo FeatureCollection cho các đơn vị Khoa học & Công nghệ
CREATE OR REPLACE VIEW v_science_geojson AS
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'type', 'Feature',
                'geometry', ST_AsGeoJSON(geom)::jsonb,
                'properties', jsonb_build_object(
                    'id', id,
                    'name', name,
                    'unitType', unit_type,
                    'wardCode', ward_code,
                    'imageUrl', image_url
                )
            )
        ),
        '[]'::jsonb
    )
)::text AS geojson
FROM science_units
WHERE geom IS NOT NULL;
