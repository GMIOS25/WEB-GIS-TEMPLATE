CREATE TABLE ocop_products (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    name varchar(255) NOT NULL,
    product_types text[] DEFAULT '{}',          -- Mảng chứa danh sách các loại sản phẩm
    star_rating integer,
    contact_phone varchar(13),                  -- Sửa dấu ; thành ,
    location_address text,
    ward_code varchar(20) NOT NULL,
    geom geometry(Point, 4326) NOT NULL,
    image_url varchar(500),
    PRIMARY KEY (id),
    CONSTRAINT ocop_products_ward_code_fkey FOREIGN KEY (ward_code) REFERENCES wards (code)
);

CREATE INDEX idx_ocop_products_ward_code ON public.ocop_products USING btree (ward_code);
CREATE INDEX idx_ocop_products_geom ON public.ocop_products USING gist (geom);
CREATE INDEX idx_ocop_products_types ON public.ocop_products USING gin (product_types); -- Chỉ mục GIN tìm kiếm mảng siêu nhanh
CREATE INDEX idx_ocop_products_geog ON public.ocop_products USING gist (CAST(geom AS geography));