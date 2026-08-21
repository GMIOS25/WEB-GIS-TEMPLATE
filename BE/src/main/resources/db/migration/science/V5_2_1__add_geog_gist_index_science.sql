CREATE INDEX idx_science_units_geog ON public.science_units USING gist (CAST(geom AS geography));
