CREATE TABLE agriculture_units (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    name varchar(255) NOT NULL,
    unit_type varchar(100),
    description text,
    ward_code varchar(20) NOT NULL,
    geom geometry(Point, 4326) NOT NULL,
    image_url varchar(500),
    PRIMARY KEY (id),
    CONSTRAINT agriculture_units_ward_code_fkey FOREIGN KEY (ward_code) REFERENCES wards (code)
);
CREATE INDEX idx_agriculture_units_ward_code ON public.agriculture_units USING btree (ward_code);
CREATE INDEX idx_agriculture_units_geom ON public.agriculture_units USING gist (geom);
