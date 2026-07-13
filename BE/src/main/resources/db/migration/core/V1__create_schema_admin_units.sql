-- Active: 1782980798161@@127.0.0.1@5432@gialai
-- DROP TABLE IF EXISTS wards;
-- DROP TABLE IF EXISTS provinces;
-- DROP TABLE IF EXISTS administrative_units;
-- DROP TABLE IF EXISTS administrative_regions;

-- CREATE administrative_regions TABLE
CREATE TABLE administrative_regions (
    id integer NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255) NOT NULL,
    code_name varchar(255) NULL,
    code_name_en varchar(255) NULL,
    CONSTRAINT administrative_regions_pkey PRIMARY KEY (id)
);

-- CREATE administrative_units TABLE
CREATE TABLE administrative_units (
    id integer NOT NULL,
    full_name varchar(255) NULL,
    full_name_en varchar(255) NULL,
    short_name varchar(255) NULL,
    short_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    code_name_en varchar(255) NULL,
    CONSTRAINT administrative_units_pkey PRIMARY KEY (id)
);

-- CREATE provinces TABLE
CREATE TABLE provinces (
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255) NULL,
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    administrative_unit_id integer NULL,
    CONSTRAINT provinces_pkey PRIMARY KEY (code)
);

-- provinces foreign keys
ALTER TABLE provinces
ADD CONSTRAINT provinces_administrative_unit_id_fkey FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units (id);

CREATE INDEX idx_provinces_unit ON provinces (administrative_unit_id);

-- CREATE wards TABLE
CREATE TABLE wards (
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    name_en varchar(255) NULL,
    full_name varchar(255) NULL,
    full_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    province_code varchar(20) NULL,
    administrative_unit_id integer NULL,
    CONSTRAINT wards_pkey PRIMARY KEY (code)
);

-- wards foreign keys

ALTER TABLE wards
ADD CONSTRAINT wards_administrative_unit_id_fkey FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units (id);

ALTER TABLE wards
ADD CONSTRAINT wards_province_code_fkey FOREIGN KEY (province_code) REFERENCES provinces (code);

CREATE INDEX idx_wards_province ON wards (province_code);

CREATE INDEX idx_wards_unit ON wards (administrative_unit_id);

CREATE TABLE users (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username varchar(50) UNIQUE NOT NULL,
    password varchar(100) NOT NULL, -- Mật khẩu mã hóa bcrypt
    full_name varchar(100),
    role varchar(20) NOT NULL CHECK (role IN ('ADMIN', 'VIEWER'))
);

-- 2. Bảng tham chiếu Lãnh đạo (Chỉ định nghĩa cấu trúc bảng để dùng sau này)
CREATE TABLE local_leaders (
    id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    full_name varchar(255) NOT NULL,
    position varchar(100) NOT NULL,
    phone_number varchar(20),
    ward_code varchar(20) REFERENCES wards (code)
);