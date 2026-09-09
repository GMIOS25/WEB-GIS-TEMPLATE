-- Ward detail loads leaders by ward_code on every request.
CREATE INDEX IF NOT EXISTS idx_local_leaders_ward_code
    ON local_leaders (ward_code);

-- The domain model and repository methods treat these relationships as 1:1.
-- Unique indexes both enforce that invariant and give the planner an exact lookup.
CREATE UNIQUE INDEX IF NOT EXISTS ux_gis_wards_ward_code
    ON gis_wards (ward_code);

CREATE UNIQUE INDEX IF NOT EXISTS ux_gis_provinces_province_code
    ON gis_provinces (province_code);
