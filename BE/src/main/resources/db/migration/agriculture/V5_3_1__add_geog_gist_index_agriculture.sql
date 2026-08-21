CREATE INDEX idx_agriculture_units_geog ON public.agriculture_units USING gist (CAST(geom AS geography));
