-- View tạo FeatureCollection cho các sản phẩm OCOP
CREATE OR REPLACE VIEW v_ocop_geojson AS
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
                    'productTypes', to_jsonb(product_types),
                    'productType', CASE WHEN product_types IS NOT NULL AND cardinality(product_types) > 0 THEN product_types[1] ELSE NULL END,
                    'starRating', star_rating,
                    'contactPhone', contact_phone,
                    'locationAddress', location_address,
                    'wardCode', ward_code,
                    'imageUrl', image_url
                )
            )
        ),
        '[]'::jsonb
    )
)::text AS geojson
FROM ocop_products
WHERE geom IS NOT NULL;
