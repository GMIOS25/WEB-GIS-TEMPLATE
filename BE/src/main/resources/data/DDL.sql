CREATE TABLE administrative_regions (
    id integer NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255) NOT NULL,
    code_name varchar(255),
    code_name_en varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE administrative_units (
    id integer NOT NULL,
    full_name varchar(255),
    full_name_en varchar(255),
    short_name varchar(255),
    short_name_en varchar(255),
    code_name varchar(255),
    code_name_en varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE gis_provinces (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    province_code varchar(20) NOT NULL,
    gis_server_id varchar(50),
    area_km2 numeric(12, 5),
    bbox geometry,
    geom geometry,
    PRIMARY KEY (id),
    CONSTRAINT gis_provinces_province_code_fkey FOREIGN key (province_code) REFERENCES provinces (code)
);

CREATE INDEX idx_gis_provinces_province_code ON public.gis_provinces USING btree (province_code);

CREATE INDEX idx_gis_provinces_bbox ON public.gis_provinces USING gist (bbox);

CREATE INDEX idx_gis_provinces_geom ON public.gis_provinces USING gist (geom);

CREATE TABLE gis_wards (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    ward_code varchar(20) NOT NULL,
    gis_server_id varchar(50),
    area_km2 numeric(12, 5),
    bbox geometry,
    geom geometry,
    PRIMARY KEY (id),
    CONSTRAINT gis_wards_ward_code_fkey FOREIGN key (ward_code) REFERENCES wards (code)
);

CREATE INDEX idx_gis_wards_ward_code ON public.gis_wards USING btree (ward_code);

CREATE INDEX idx_gis_wards_bbox ON public.gis_wards USING gist (bbox);

CREATE INDEX idx_gis_wards_geom ON public.gis_wards USING gist (geom);

CREATE TABLE local_leaders (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    full_name varchar(255) NOT NULL,
    position varchar(100) NOT NULL,
    phone_number varchar(20),
    ward_code varchar(20),
    PRIMARY KEY (id),
    CONSTRAINT local_leaders_ward_code_fkey FOREIGN key (ward_code) REFERENCES wards (code)
);

CREATE TABLE provinces (
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255),
    code_name varchar(255),
    administrative_unit_id integer,
    PRIMARY KEY (code),
    CONSTRAINT provinces_administrative_unit_id_fkey FOREIGN key (administrative_unit_id) REFERENCES administrative_units (id)
);

CREATE INDEX idx_provinces_unit ON public.provinces USING btree (administrative_unit_id);

CREATE TABLE spatial_ref_sys (
    srid integer NOT NULL,
    auth_name varchar(256),
    auth_srid integer,
    srtext varchar(2048),
    proj4text varchar(2048),
    PRIMARY KEY (srid),
    CONSTRAINT spatial_ref_sys_srid_check CHECK (
        (srid > 0)
        AND (srid <= 998999)
    )
);

CREATE TABLE users (
    id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    username varchar(50) NOT NULL,
    password varchar(100) NOT NULL,
    full_name varchar(100),
    role varchar(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT users_role_check CHECK (
        (role)::text = ANY (
            (
                ARRAY[
                    'ADMIN'::character varying,
                    'VIEWER'::character varying
                ]
            )::text []
        )
    )
);

CREATE UNIQUE INDEX users_username_key ON public.users USING btree (username);

CREATE TABLE wards (
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255),
    full_name varchar(255),
    full_name_en varchar(255),
    code_name varchar(255),
    province_code varchar(20),
    administrative_unit_id integer,
    PRIMARY KEY (code),
    CONSTRAINT wards_administrative_unit_id_fkey FOREIGN key (administrative_unit_id) REFERENCES administrative_units (id),
    CONSTRAINT wards_province_code_fkey FOREIGN key (province_code) REFERENCES provinces (code)
);

CREATE INDEX idx_wards_province ON public.wards USING btree (province_code);

CREATE INDEX idx_wards_unit ON public.wards USING btree (administrative_unit_id);