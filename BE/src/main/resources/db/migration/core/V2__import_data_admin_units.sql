/* === Filtered Vietnamese Provinces Database for Gia Lai === */
/* Reference: Filtered from template */

-- DATA for administrative_regions --
INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        1,
        'Đông Bắc Bộ',
        'Northeast',
        'dong_bac_bo',
        'northest'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        2,
        'Tây Bắc Bộ',
        'Northwest',
        'tay_bac_bo',
        'northwest'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        3,
        'Đồng bằng sông Hồng',
        'Red River Delta',
        'dong_bang_song_hong',
        'red_river_delta'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        4,
        'Bắc Trung Bộ',
        'North Central Coast',
        'bac_trung_bo',
        'north_central_coast'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        5,
        'Duyên hải Nam Trung Bộ',
        'South Central Coast',
        'duyen_hai_nam_trung_bo',
        'south_central_coast'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        6,
        'Tây Nguyên',
        'Central Highlands',
        'tay_nguyen',
        'central_highlands'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        7,
        'Đông Nam Bộ',
        'Southeast',
        'dong_nam_bo',
        'southeast'
    );

INSERT INTO
    administrative_regions (
        id,
        name,
        name_en,
        code_name,
        code_name_en
    )
VALUES (
        8,
        'Đồng bằng sông Cửu Long',
        'Mekong River Delta',
        'dong_bang_song_cuu_long',
        'southwest'
    );
-- ----------------------------------

-- DATA for administrative_units --
INSERT INTO
    administrative_units (
        id,
        full_name,
        full_name_en,
        short_name,
        short_name_en,
        code_name,
        code_name_en
    )
VALUES (
        1,
        'Thành phố trực thuộc trung ương',
        'Municipality',
        'Thành phố',
        'City',
        'thanh_pho_truc_thuoc_trung_uong',
        'municipality'
    );

INSERT INTO
    administrative_units (
        id,
        full_name,
        full_name_en,
        short_name,
        short_name_en,
        code_name,
        code_name_en
    )
VALUES (
        2,
        'Tỉnh',
        'Province',
        'Tỉnh',
        'Province',
        'tinh',
        'province'
    );

INSERT INTO
    administrative_units (
        id,
        full_name,
        full_name_en,
        short_name,
        short_name_en,
        code_name,
        code_name_en
    )
VALUES (
        3,
        'Phường',
        'Ward',
        'Phường',
        'Ward',
        'phuong',
        'ward'
    );

INSERT INTO
    administrative_units (
        id,
        full_name,
        full_name_en,
        short_name,
        short_name_en,
        code_name,
        code_name_en
    )
VALUES (
        4,
        'Xã',
        'Commune',
        'Xã',
        'Commune',
        'xa',
        'commune'
    );

INSERT INTO
    administrative_units (
        id,
        full_name,
        full_name_en,
        short_name,
        short_name_en,
        code_name,
        code_name_en
    )
VALUES (
        5,
        'Đặc khu tại hải đảo',
        'Special administrative region',
        'Đặc khu',
        'Special administrative region',
        'dac_khu',
        'special_administrative_region'
    );
-- ----------------------------------

-- DATA for provinces --
INSERT INTO
    provinces (
        code,
        name,
        name_en,
        full_name,
        full_name_en,
        code_name,
        administrative_unit_id
    )
VALUES (
        '52',
        'Gia Lai',
        'Gia Lai',
        'Tỉnh Gia Lai',
        'Gia Lai Province',
        'gia_lai',
        2
    );

-- DATA for wards --
INSERT INTO
    wards (
        code,
        name,
        name_en,
        full_name,
        full_name_en,
        code_name,
        province_code,
        administrative_unit_id
    )
VALUES (
        '21553',
        'Quy Nhơn Bắc',
        'Quy Nhon Bac',
        'Phường Quy Nhơn Bắc',
        'Quy Nhon Bac Ward',
        'quy_nhon_bac',
        '52',
        3
    ),
    (
        '21583',
        'Quy Nhơn',
        'Quy Nhon',
        'Phường Quy Nhơn',
        'Quy Nhon Ward',
        'quy_nhon',
        '52',
        3
    ),
    (
        '21589',
        'Quy Nhơn Tây',
        'Quy Nhon Tay',
        'Phường Quy Nhơn Tây',
        'Quy Nhon Tay Ward',
        'quy_nhon_tay',
        '52',
        3
    ),
    (
        '21592',
        'Quy Nhơn Nam',
        'Quy Nhon Nam',
        'Phường Quy Nhơn Nam',
        'Quy Nhon Nam Ward',
        'quy_nhon_nam',
        '52',
        3
    ),
    (
        '21601',
        'Quy Nhơn Đông',
        'Quy Nhon Dong',
        'Phường Quy Nhơn Đông',
        'Quy Nhon Dong Ward',
        'quy_nhon_dong',
        '52',
        3
    ),
    (
        '21607',
        'Nhơn Châu',
        'Nhon Chau',
        'Xã Nhơn Châu',
        'Nhon Chau Commune',
        'nhon_chau',
        '52',
        4
    ),
    (
        '21609',
        'An Lão',
        'An Lao',
        'Xã An Lão',
        'An Lao Commune',
        'an_lao',
        '52',
        4
    ),
    (
        '21616',
        'An Vinh',
        'An Vinh',
        'Xã An Vinh',
        'An Vinh Commune',
        'an_vinh',
        '52',
        4
    ),
    (
        '21622',
        'An Toàn',
        'An Toan',
        'Xã An Toàn',
        'An Toan Commune',
        'an_toan',
        '52',
        4
    ),
    (
        '21628',
        'An Hoà',
        'An Hoa',
        'Xã An Hoà',
        'An Hoa Commune',
        'an_hoa',
        '52',
        4
    ),
    (
        '21637',
        'Tam Quan',
        'Tam Quan',
        'Phường Tam Quan',
        'Tam Quan Ward',
        'tam_quan',
        '52',
        3
    ),
    (
        '21640',
        'Bồng Sơn',
        'Bong Son',
        'Phường Bồng Sơn',
        'Bong Son Ward',
        'bong_son',
        '52',
        3
    ),
    (
        '21655',
        'Hoài Nhơn Bắc',
        'Hoai Nhon Bac',
        'Phường Hoài Nhơn Bắc',
        'Hoai Nhon Bac Ward',
        'hoai_nhon_bac',
        '52',
        3
    ),
    (
        '21661',
        'Hoài Nhơn Tây',
        'Hoai Nhon Tay',
        'Phường Hoài Nhơn Tây',
        'Hoai Nhon Tay Ward',
        'hoai_nhon_tay',
        '52',
        3
    ),
    (
        '21664',
        'Hoài Nhơn',
        'Hoai Nhon',
        'Phường Hoài Nhơn',
        'Hoai Nhon Ward',
        'hoai_nhon',
        '52',
        3
    ),
    (
        '21670',
        'Hoài Nhơn Đông',
        'Hoai Nhon Dong',
        'Phường Hoài Nhơn Đông',
        'Hoai Nhon Dong Ward',
        'hoai_nhon_dong',
        '52',
        3
    ),
    (
        '21673',
        'Hoài Nhơn Nam',
        'Hoai Nhon Nam',
        'Phường Hoài Nhơn Nam',
        'Hoai Nhon Nam Ward',
        'hoai_nhon_nam',
        '52',
        3
    ),
    (
        '21688',
        'Hoài Ân',
        'Hoai An',
        'Xã Hoài Ân',
        'Hoai An Commune',
        'hoai_an',
        '52',
        4
    ),
    (
        '21697',
        'Ân Hảo',
        'An Hao',
        'Xã Ân Hảo',
        'An Hao Commune',
        'an_hao',
        '52',
        4
    ),
    (
        '21703',
        'Vạn Đức',
        'Van Duc',
        'Xã Vạn Đức',
        'Van Duc Commune',
        'van_duc',
        '52',
        4
    ),
    (
        '21715',
        'Ân Tường',
        'An Tuong',
        'Xã Ân Tường',
        'An Tuong Commune',
        'an_tuong',
        '52',
        4
    ),
    (
        '21727',
        'Kim Sơn',
        'Kim Son',
        'Xã Kim Sơn',
        'Kim Son Commune',
        'kim_son',
        '52',
        4
    ),
    (
        '21730',
        'Phù Mỹ',
        'Phu My',
        'Xã Phù Mỹ',
        'Phu My Commune',
        'phu_my',
        '52',
        4
    ),
    (
        '21733',
        'Bình Dương',
        'Binh Duong',
        'Xã Bình Dương',
        'Binh Duong Commune',
        'binh_duong',
        '52',
        4
    ),
    (
        '21739',
        'Phù Mỹ Bắc',
        'Phu My Bac',
        'Xã Phù Mỹ Bắc',
        'Phu My Bac Commune',
        'phu_my_bac',
        '52',
        4
    ),
    (
        '21751',
        'Phù Mỹ Đông',
        'Phu My Dong',
        'Xã Phù Mỹ Đông',
        'Phu My Dong Commune',
        'phu_my_dong',
        '52',
        4
    ),
    (
        '21757',
        'Phù Mỹ Tây',
        'Phu My Tay',
        'Xã Phù Mỹ Tây',
        'Phu My Tay Commune',
        'phu_my_tay',
        '52',
        4
    ),
    (
        '21769',
        'An Lương',
        'An Luong',
        'Xã An Lương',
        'An Luong Commune',
        'an_luong',
        '52',
        4
    ),
    (
        '21775',
        'Phù Mỹ Nam',
        'Phu My Nam',
        'Xã Phù Mỹ Nam',
        'Phu My Nam Commune',
        'phu_my_nam',
        '52',
        4
    ),
    (
        '21786',
        'Vĩnh Thạnh',
        'Vinh Thanh',
        'Xã Vĩnh Thạnh',
        'Vinh Thanh Commune',
        'vinh_thanh',
        '52',
        4
    ),
    (
        '21787',
        'Vĩnh Sơn',
        'Vinh Son',
        'Xã Vĩnh Sơn',
        'Vinh Son Commune',
        'vinh_son',
        '52',
        4
    ),
    (
        '21796',
        'Vĩnh Thịnh',
        'Vinh Thinh',
        'Xã Vĩnh Thịnh',
        'Vinh Thinh Commune',
        'vinh_thinh',
        '52',
        4
    ),
    (
        '21805',
        'Vĩnh Quang',
        'Vinh Quang',
        'Xã Vĩnh Quang',
        'Vinh Quang Commune',
        'vinh_quang',
        '52',
        4
    ),
    (
        '21808',
        'Tây Sơn',
        'Tay Son',
        'Xã Tây Sơn',
        'Tay Son Commune',
        'tay_son',
        '52',
        4
    ),
    (
        '21817',
        'Bình Hiệp',
        'Binh Hiep',
        'Xã Bình Hiệp',
        'Binh Hiep Commune',
        'binh_hiep',
        '52',
        4
    ),
    (
        '21820',
        'Bình Khê',
        'Binh Khe',
        'Xã Bình Khê',
        'Binh Khe Commune',
        'binh_khe',
        '52',
        4
    ),
    (
        '21829',
        'Bình An',
        'Binh An',
        'Xã Bình An',
        'Binh An Commune',
        'binh_an',
        '52',
        4
    ),
    (
        '21835',
        'Bình Phú',
        'Binh Phu',
        'Xã Bình Phú',
        'Binh Phu Commune',
        'binh_phu',
        '52',
        4
    ),
    (
        '21853',
        'Phù Cát',
        'Phu Cat',
        'Xã Phù Cát',
        'Phu Cat Commune',
        'phu_cat',
        '52',
        4
    ),
    (
        '21859',
        'Đề Gi',
        'De Gi',
        'Xã Đề Gi',
        'De Gi Commune',
        'de_gi',
        '52',
        4
    ),
    (
        '21868',
        'Hội Sơn',
        'Hoi Son',
        'Xã Hội Sơn',
        'Hoi Son Commune',
        'hoi_son',
        '52',
        4
    ),
    (
        '21871',
        'Hoà Hội',
        'Hoa Hoi',
        'Xã Hoà Hội',
        'Hoa Hoi Commune',
        'hoa_hoi',
        '52',
        4
    ),
    (
        '21880',
        'Cát Tiến',
        'Cat Tien',
        'Xã Cát Tiến',
        'Cat Tien Commune',
        'cat_tien',
        '52',
        4
    ),
    (
        '21892',
        'Xuân An',
        'Xuan An',
        'Xã Xuân An',
        'Xuan An Commune',
        'xuan_an',
        '52',
        4
    ),
    (
        '21901',
        'Ngô Mây',
        'Ngo May',
        'Xã Ngô Mây',
        'Ngo May Commune',
        'ngo_may',
        '52',
        4
    ),
    (
        '21907',
        'Bình Định',
        'Binh Dinh',
        'Phường Bình Định',
        'Binh Dinh Ward',
        'binh_dinh',
        '52',
        3
    ),
    (
        '21910',
        'An Nhơn',
        'An Nhon',
        'Phường An Nhơn',
        'An Nhon Ward',
        'an_nhon',
        '52',
        3
    ),
    (
        '21925',
        'An Nhơn Bắc',
        'An Nhon Bac',
        'Phường An Nhơn Bắc',
        'An Nhon Bac Ward',
        'an_nhon_bac',
        '52',
        3
    ),
    (
        '21934',
        'An Nhơn Đông',
        'An Nhon Dong',
        'Phường An Nhơn Đông',
        'An Nhon Dong Ward',
        'an_nhon_dong',
        '52',
        3
    ),
    (
        '21940',
        'An Nhơn Tây',
        'An Nhon Tay',
        'Xã An Nhơn Tây',
        'An Nhon Tay Commune',
        'an_nhon_tay',
        '52',
        4
    ),
    (
        '21943',
        'An Nhơn Nam',
        'An Nhon Nam',
        'Phường An Nhơn Nam',
        'An Nhon Nam Ward',
        'an_nhon_nam',
        '52',
        3
    ),
    (
        '21952',
        'Tuy Phước',
        'Tuy Phuoc',
        'Xã Tuy Phước',
        'Tuy Phuoc Commune',
        'tuy_phuoc',
        '52',
        4
    ),
    (
        '21964',
        'Tuy Phước Bắc',
        'Tuy Phuoc Bac',
        'Xã Tuy Phước Bắc',
        'Tuy Phuoc Bac Commune',
        'tuy_phuoc_bac',
        '52',
        4
    ),
    (
        '21970',
        'Tuy Phước Đông',
        'Tuy Phuoc Dong',
        'Xã Tuy Phước Đông',
        'Tuy Phuoc Dong Commune',
        'tuy_phuoc_dong',
        '52',
        4
    ),
    (
        '21985',
        'Tuy Phước Tây',
        'Tuy Phuoc Tay',
        'Xã Tuy Phước Tây',
        'Tuy Phuoc Tay Commune',
        'tuy_phuoc_tay',
        '52',
        4
    ),
    (
        '21994',
        'Vân Canh',
        'Van Canh',
        'Xã Vân Canh',
        'Van Canh Commune',
        'van_canh',
        '52',
        4
    ),
    (
        '21997',
        'Canh Liên',
        'Canh Lien',
        'Xã Canh Liên',
        'Canh Lien Commune',
        'canh_lien',
        '52',
        4
    ),
    (
        '22006',
        'Canh Vinh',
        'Canh Vinh',
        'Xã Canh Vinh',
        'Canh Vinh Commune',
        'canh_vinh',
        '52',
        4
    ),
    (
        '23563',
        'Diên Hồng',
        'Dien Hong',
        'Phường Diên Hồng',
        'Dien Hong Ward',
        'dien_hong',
        '52',
        3
    ),
    (
        '23575',
        'Pleiku',
        'Pleiku',
        'Phường Pleiku',
        'Pleiku Ward',
        'pleiku',
        '52',
        3
    ),
    (
        '23584',
        'Thống Nhất',
        'Thong Nhat',
        'Phường Thống Nhất',
        'Thong Nhat Ward',
        'thong_nhat',
        '52',
        3
    ),
    (
        '23586',
        'Hội Phú',
        'Hoi Phu',
        'Phường Hội Phú',
        'Hoi Phu Ward',
        'hoi_phu',
        '52',
        3
    ),
    (
        '23590',
        'Biển Hồ',
        'Bien Ho',
        'Xã Biển Hồ',
        'Bien Ho Commune',
        'bien_ho',
        '52',
        4
    ),
    (
        '23602',
        'An Phú',
        'An Phu',
        'Phường An Phú',
        'An Phu Ward',
        'an_phu',
        '52',
        3
    ),
    (
        '23611',
        'Gào',
        'Gao',
        'Xã Gào',
        'Gao Commune',
        'gao',
        '52',
        4
    ),
    (
        '23614',
        'An Bình',
        'An Binh',
        'Phường An Bình',
        'An Binh Ward',
        'an_binh',
        '52',
        3
    ),
    (
        '23617',
        'An Khê',
        'An Khe',
        'Phường An Khê',
        'An Khe Ward',
        'an_khe',
        '52',
        3
    ),
    (
        '23629',
        'Cửu An',
        'Cuu An',
        'Xã Cửu An',
        'Cuu An Commune',
        'cuu_an',
        '52',
        4
    ),
    (
        '23638',
        'Kbang',
        'Kbang',
        'Xã Kbang',
        'Kbang Commune',
        'kbang',
        '52',
        4
    ),
    (
        '23644',
        'Đak Rong',
        'Dak Rong',
        'Xã Đak Rong',
        'Dak Rong Commune',
        'dak_rong',
        '52',
        4
    ),
    (
        '23647',
        'Sơn Lang',
        'Son Lang',
        'Xã Sơn Lang',
        'Son Lang Commune',
        'son_lang',
        '52',
        4
    ),
    (
        '23650',
        'Krong',
        'Krong',
        'Xã Krong',
        'Krong Commune',
        'krong',
        '52',
        4
    ),
    (
        '23668',
        'Tơ Tung',
        'To Tung',
        'Xã Tơ Tung',
        'To Tung Commune',
        'to_tung',
        '52',
        4
    ),
    (
        '23674',
        'Kông Bơ La',
        'Kong Bo La',
        'Xã Kông Bơ La',
        'Kong Bo La Commune',
        'kong_bo_la',
        '52',
        4
    ),
    (
        '23677',
        'Đak Đoa',
        'Dak Doa',
        'Xã Đak Đoa',
        'Dak Doa Commune',
        'dak_doa',
        '52',
        4
    ),
    (
        '23683',
        'Đak Sơmei',
        'Dak Somei',
        'Xã Đak Sơmei',
        'Dak Somei Commune',
        'dak_somei',
        '52',
        4
    ),
    (
        '23701',
        'Kon Gang',
        'Kon Gang',
        'Xã Kon Gang',
        'Kon Gang Commune',
        'kon_gang',
        '52',
        4
    ),
    (
        '23710',
        'Ia Băng',
        'Ia Bang',
        'Xã Ia Băng',
        'Ia Bang Commune',
        'ia_bang',
        '52',
        4
    ),
    (
        '23714',
        'KDang',
        'KDang',
        'Xã KDang',
        'KDang Commune',
        'kdang',
        '52',
        4
    ),
    (
        '23722',
        'Chư Păh',
        'Chu Pah',
        'Xã Chư Păh',
        'Chu Pah Commune',
        'chu_pah',
        '52',
        4
    ),
    (
        '23728',
        'Ia Khươl',
        'Ia Khuol',
        'Xã Ia Khươl',
        'Ia Khuol Commune',
        'ia_khuol',
        '52',
        4
    ),
    (
        '23734',
        'Ia Ly',
        'Ia Ly',
        'Xã Ia Ly',
        'Ia Ly Commune',
        'ia_ly',
        '52',
        4
    ),
    (
        '23749',
        'Ia Phí',
        'Ia Phi',
        'Xã Ia Phí',
        'Ia Phi Commune',
        'ia_phi',
        '52',
        4
    ),
    (
        '23764',
        'Ia Grai',
        'Ia Grai',
        'Xã Ia Grai',
        'Ia Grai Commune',
        'ia_grai',
        '52',
        4
    ),
    (
        '23767',
        'Ia Hrung',
        'Ia Hrung',
        'Xã Ia Hrung',
        'Ia Hrung Commune',
        'ia_hrung',
        '52',
        4
    ),
    (
        '23776',
        'Ia Krái',
        'Ia Krai',
        'Xã Ia Krái',
        'Ia Krai Commune',
        'ia_krai',
        '52',
        4
    ),
    (
        '23782',
        'Ia O',
        'Ia O',
        'Xã Ia O',
        'Ia O Commune',
        'ia_o',
        '52',
        4
    ),
    (
        '23788',
        'Ia Chia',
        'Ia Chia',
        'Xã Ia Chia',
        'Ia Chia Commune',
        'ia_chia',
        '52',
        4
    ),
    (
        '23794',
        'Mang Yang',
        'Mang Yang',
        'Xã Mang Yang',
        'Mang Yang Commune',
        'mang_yang',
        '52',
        4
    ),
    (
        '23798',
        'Ayun',
        'Ayun',
        'Xã Ayun',
        'Ayun Commune',
        'ayun',
        '52',
        4
    ),
    (
        '23799',
        'Hra',
        'Hra',
        'Xã Hra',
        'Hra Commune',
        'hra',
        '52',
        4
    ),
    (
        '23812',
        'Lơ Pang',
        'Lo Pang',
        'Xã Lơ Pang',
        'Lo Pang Commune',
        'lo_pang',
        '52',
        4
    ),
    (
        '23818',
        'Kon Chiêng',
        'Kon Chieng',
        'Xã Kon Chiêng',
        'Kon Chieng Commune',
        'kon_chieng',
        '52',
        4
    ),
    (
        '23824',
        'Kông Chro',
        'Kong Chro',
        'Xã Kông Chro',
        'Kong Chro Commune',
        'kong_chro',
        '52',
        4
    ),
    (
        '23830',
        'Chư Krey',
        'Chu Krey',
        'Xã Chư Krey',
        'Chu Krey Commune',
        'chu_krey',
        '52',
        4
    ),
    (
        '23833',
        'Ya Ma',
        'Ya Ma',
        'Xã Ya Ma',
        'Ya Ma Commune',
        'ya_ma',
        '52',
        4
    ),
    (
        '23839',
        'SRó',
        'SRo',
        'Xã SRó',
        'SRo Commune',
        'sro',
        '52',
        4
    ),
    (
        '23842',
        'Đăk Song',
        'Dak Song',
        'Xã Đăk Song',
        'Dak Song Commune',
        'dak_song',
        '52',
        4
    ),
    (
        '23851',
        'Chơ Long',
        'Cho Long',
        'Xã Chơ Long',
        'Cho Long Commune',
        'cho_long',
        '52',
        4
    ),
    (
        '23857',
        'Đức Cơ',
        'Duc Co',
        'Xã Đức Cơ',
        'Duc Co Commune',
        'duc_co',
        '52',
        4
    ),
    (
        '23866',
        'Ia Krêl',
        'Ia Krel',
        'Xã Ia Krêl',
        'Ia Krel Commune',
        'ia_krel',
        '52',
        4
    ),
    (
        '23869',
        'Ia Dơk',
        'Ia Dok',
        'Xã Ia Dơk',
        'Ia Dok Commune',
        'ia_dok',
        '52',
        4
    ),
    (
        '23872',
        'Ia Dom',
        'Ia Dom',
        'Xã Ia Dom',
        'Ia Dom Commune',
        'ia_dom',
        '52',
        4
    ),
    (
        '23881',
        'Ia Pnôn',
        'Ia Pnon',
        'Xã Ia Pnôn',
        'Ia Pnon Commune',
        'ia_pnon',
        '52',
        4
    ),
    (
        '23884',
        'Ia Nan',
        'Ia Nan',
        'Xã Ia Nan',
        'Ia Nan Commune',
        'ia_nan',
        '52',
        4
    ),
    (
        '23887',
        'Chư Prông',
        'Chu Prong',
        'Xã Chư Prông',
        'Chu Prong Commune',
        'chu_prong',
        '52',
        4
    ),
    (
        '23896',
        'Bàu Cạn',
        'Bau Can',
        'Xã Bàu Cạn',
        'Bau Can Commune',
        'bau_can',
        '52',
        4
    ),
    (
        '23908',
        'Ia Tôr',
        'Ia Tor',
        'Xã Ia Tôr',
        'Ia Tor Commune',
        'ia_tor',
        '52',
        4
    ),
    (
        '23911',
        'Ia Boòng',
        'Ia Boong',
        'Xã Ia Boòng',
        'Ia Boong Commune',
        'ia_boong',
        '52',
        4
    ),
    (
        '23917',
        'Ia Púch',
        'Ia Puch',
        'Xã Ia Púch',
        'Ia Puch Commune',
        'ia_puch',
        '52',
        4
    ),
    (
        '23926',
        'Ia Pia',
        'Ia Pia',
        'Xã Ia Pia',
        'Ia Pia Commune',
        'ia_pia',
        '52',
        4
    ),
    (
        '23935',
        'Ia Lâu',
        'Ia Lau',
        'Xã Ia Lâu',
        'Ia Lau Commune',
        'ia_lau',
        '52',
        4
    ),
    (
        '23938',
        'Ia Mơ',
        'Ia Mo',
        'Xã Ia Mơ',
        'Ia Mo Commune',
        'ia_mo',
        '52',
        4
    ),
    (
        '23941',
        'Chư Sê',
        'Chu Se',
        'Xã Chư Sê',
        'Chu Se Commune',
        'chu_se',
        '52',
        4
    ),
    (
        '23942',
        'Chư Pưh',
        'Chu Puh',
        'Xã Chư Pưh',
        'Chu Puh Commune',
        'chu_puh',
        '52',
        4
    ),
    (
        '23947',
        'Bờ Ngoong',
        'Bo Ngoong',
        'Xã Bờ Ngoong',
        'Bo Ngoong Commune',
        'bo_ngoong',
        '52',
        4
    ),
    (
        '23954',
        'Al Bá',
        'Al Ba',
        'Xã Al Bá',
        'Al Ba Commune',
        'al_ba',
        '52',
        4
    ),
    (
        '23971',
        'Ia Hrú',
        'Ia Hru',
        'Xã Ia Hrú',
        'Ia Hru Commune',
        'ia_hru',
        '52',
        4
    ),
    (
        '23977',
        'Ia Ko',
        'Ia Ko',
        'Xã Ia Ko',
        'Ia Ko Commune',
        'ia_ko',
        '52',
        4
    ),
    (
        '23986',
        'Ia Le',
        'Ia Le',
        'Xã Ia Le',
        'Ia Le Commune',
        'ia_le',
        '52',
        4
    ),
    (
        '23995',
        'Đak Pơ',
        'Dak Po',
        'Xã Đak Pơ',
        'Dak Po Commune',
        'dak_po',
        '52',
        4
    ),
    (
        '24007',
        'Ya Hội',
        'Ya Hoi',
        'Xã Ya Hội',
        'Ya Hoi Commune',
        'ya_hoi',
        '52',
        4
    ),
    (
        '24013',
        'Pờ Tó',
        'Po To',
        'Xã Pờ Tó',
        'Po To Commune',
        'po_to',
        '52',
        4
    ),
    (
        '24022',
        'Ia Pa',
        'Ia Pa',
        'Xã Ia Pa',
        'Ia Pa Commune',
        'ia_pa',
        '52',
        4
    ),
    (
        '24028',
        'Ia Tul',
        'Ia Tul',
        'Xã Ia Tul',
        'Ia Tul Commune',
        'ia_tul',
        '52',
        4
    ),
    (
        '24043',
        'Phú Thiện',
        'Phu Thien',
        'Xã Phú Thiện',
        'Phu Thien Commune',
        'phu_thien',
        '52',
        4
    ),
    (
        '24044',
        'Ayun Pa',
        'Ayun Pa',
        'Phường Ayun Pa',
        'Ayun Pa Ward',
        'ayun_pa',
        '52',
        3
    ),
    (
        '24049',
        'Chư A Thai',
        'Chu A Thai',
        'Xã Chư A Thai',
        'Chu A Thai Commune',
        'chu_a_thai',
        '52',
        4
    ),
    (
        '24061',
        'Ia Hiao',
        'Ia Hiao',
        'Xã Ia Hiao',
        'Ia Hiao Commune',
        'ia_hiao',
        '52',
        4
    ),
    (
        '24065',
        'Ia Rbol',
        'Ia Rbol',
        'Xã Ia Rbol',
        'Ia Rbol Commune',
        'ia_rbol',
        '52',
        4
    ),
    (
        '24073',
        'Ia Sao',
        'Ia Sao',
        'Xã Ia Sao',
        'Ia Sao Commune',
        'ia_sao',
        '52',
        4
    ),
    (
        '24076',
        'Phú Túc',
        'Phu Tuc',
        'Xã Phú Túc',
        'Phu Tuc Commune',
        'phu_tuc',
        '52',
        4
    ),
    (
        '24100',
        'Ia Dreh',
        'Ia Dreh',
        'Xã Ia Dreh',
        'Ia Dreh Commune',
        'ia_dreh',
        '52',
        4
    ),
    (
        '24109',
        'Uar',
        'Uar',
        'Xã Uar',
        'Uar Commune',
        'uar',
        '52',
        4
    ),
    (
        '24112',
        'Ia Rsai',
        'Ia Rsai',
        'Xã Ia Rsai',
        'Ia Rsai Commune',
        'ia_rsai',
        '52',
        4
    );

INSERT INTO local_leaders (full_name, position, phone_number, ward_code) VALUES 
('NGUYỄN ĐỨC TOÀN', 'Chủ tịch', '0914115114', '21583'),
('NGUYỄN THỊ BÍCH HOA', 'Chủ tịch', '0913759467', '21592'),
('NGUYỄN THÁI DIỄN', 'Chủ tịch', '0905065968', '21553'),
('NGUYỄN QUANG MINH', 'Chủ tịch', '0934848428', '21589'),
('TRẦN VIỆT QUANG', 'Chủ tịch', '0935250065', '21601'),
('DƯƠNG HIỆP HƯNG', 'Chủ tịch', '0708077929', '21607'),
('LÊ QUỐC CƯỜNG', 'Chủ tịch', '0919467799', '21907'),
('NGUYỄN ĐÌNH CHƯƠNG', 'Chủ tịch', '0914429669', '21910'),
('MAI XUÂN TIẾN', 'Chủ tịch', '0982211393', '21934'),
('LÊ HOÀNG HIỆP', 'Chủ tịch', '0985013647', '21940'),
('LÊ HOÀI AN', 'Chủ tịch', '0983835349', '21943'),
('NGUYỄN ANH DŨNG', 'Chủ tịch', '0932467799', '21925'),
('TRƯƠNG NAM PHONG', 'Chủ tịch', '0913615399', '21640'),
('TRẦN VĂN THƯ', 'Chủ tịch', '0965213668', '21673'),
('NGUYỄN VĂN HIỆP', 'Chủ tịch', '0973121315', '21670'),
('ĐẶNG ĐỨC TOÀN', 'Chủ tịch', '0932433145', '21664'),
('NGUYỄN LÊ ANH TUẤN', 'Chủ tịch', '0917470199', '21661'),
('TRƯƠNG THỊ THÚY ỨC', 'Chủ tịch', '0938074899', '21637'),
('NGUYỄN VĂN HÓA', 'Chủ tịch', '0972171125', '21655'),
('BÙI QUỐC NGHỊ', 'Chủ tịch', '0914070219', '21853'),
('NGUYỄN PHÚC LINH', 'Chủ tịch', '0914123018', '21892'),
('NGUYỄN VĂN LÊ', 'Chủ tịch', '0978042712', '21901'),
('VÕ VĂN TRỊNH', 'Chủ tịch', '0914124677', '21880'),
('VÕ VĂN TÀI', 'Chủ tịch', '0983991700', '21859'),
('ĐINH THÀNH TIẾN', 'Chủ tịch', '0988808777', '21871'),
('HÀ TRỌNG DƯỠNG', 'Chủ tịch', '0919161664', '21868'),
('TRẦN MINH TUẤN', 'Chủ tịch', '0982856154', '21730'),
('NGÔ VŨ HẢI', 'Chủ tịch', '0868675221', '21757'),
('NGUYỄN THỊ THU PHƯƠNG', 'Chủ tịch', '0919425364', '21775'),
('VÕ THANH HOÀNG', 'Chủ tịch', '0978437510', '21769'),
('TRẦN MINH THÔNG', 'Chủ tịch', '0936666589', '21751'),
('ĐINH CÔNG HOÀNG', 'Chủ tịch', '0905217935', '21733'),
('ĐẶNG ĐÌNH TRIỀU', 'Chủ tịch', '0942770999', '21739'),
('TRẦN MINH QUANG', 'Chủ tịch', '0947258889', '21952'),
('TÔ MINH CHÁNH', 'Chủ tịch', '0966221479', '21985'),
('NGUYỄN HÙNG TÂN', 'Chủ tịch', '0905979268', '21964'),
('DƯƠNG MINH TÂN', 'Chủ tịch', '0937676379', '21970'),
('NGUYỄN THỊ THỐNG', 'Chủ tịch', '0961492492', '21808'),
('NGUYỄN CÔNG ĐỆ', 'Chủ tịch', '0977670679', '21820'),
('LÝ KIM ĐÌNH', 'Chủ tịch', '098525765', '21835'),
('LÊ HÀ AN', 'Chủ tịch', '0905880223', '21817'),
('MAI XUÂN HẬU', 'Chủ tịch', '0986956910', '21829'),
('HOÀNG ANH NGỌC', 'Chủ tịch', '0985227758', '21688'),
('VŨ THÀNH MINH', 'Chủ tịch', '0964327789', '21715'),
('NGUYỄN VĂN THIỆN', 'Chủ tịch', '0913222489', '21727'),
('NGUYỄN HOÀI NGUYÊN', 'Chủ tịch', '0972157176', '21703'),
('NGUYỄN XUÂN VIỆT', 'Chủ tịch', '0935590404', '21994'),
('DƯƠNG HIỆP HÒA', 'Chủ tịch', '0905180630', '22006'),
('HOÀNG THỊ MINH BÌNH', 'Chủ tịch', '0905157175', '23602'),
('NGUYỄN HOÀI NAM', 'Chủ tịch', '0914429662', '21997'),
('ĐẶNG QUANG KHANH', 'Chủ tịch', '0988158180', '24044'),
('LÊ MINH THÔNG', 'Chủ tịch', '0914188915', '21786'),
('NGUYỄN TUẤN QUANG', 'Chủ tịch', '0905171397', '23563'),
('HUỲNH ĐỨC BẢO', 'Chủ tịch', '0977699866', '21796'),
('VŨ MẠNH ĐỊNH', 'Chủ tịch', '0982909909', '23586'),
('TRỊNH THÀNH TRUNG', 'Chủ tịch', '0988670903', '21805'),
('ĐẶNG TOÀN THẮNG', 'Chủ tịch', '0914099033', '23575'),
('NGUYỄN VĂN PHÚNG', 'Chủ tịch', '0914739047', '21787'),
('PHẠM THẾ TÂM', 'Chủ tịch', '0913412688', '23584'),
('HUỲNH TÂN', 'Chủ tịch', '0977478355', '21628'),
('NGÔ XUÂN HIẾU', 'Chủ tịch', '0914032929', '23954'),
('LÊ VĂN THANH', 'Chủ tịch', '0977813062', '21609'),
('NGUYỄN XUÂN CAN', 'Chủ tịch', '0974216109', '23798'),
('ĐINH VĂN CUNG', 'Chủ tịch', '0963444149', '21616'),
('NGUYỄN QUANG TRUNG', 'Chủ tịch', '0912044359', '23896'),
('VÕ DUY TÍN', 'Chủ tịch', '0988858957', '21697'),
('TRỊNH VĂN SANG', 'Chủ tịch', '0969022299', '23590'),
('ĐINH VĂN NGHIN', 'Chủ tịch', '0359340578', '21622'),
('NGUYỄN HỮU TỴ', 'Chủ tịch', '0983315247', '23947'),
('PHẠM ĐÌNH LONG', 'Chủ tịch', '0977284775', '23851'),
('PHẠM VĂN LƯỢNG', 'Chủ tịch', '0914096272', '24049'),
('NGUYỄN KHẮC MINH', 'Chủ tịch', '0905855112', '23830'),
('TRẦN ANH TUẤN', 'Chủ tịch', '0979425228', '23722'),
('PHẠM NGỌC TOÀN', 'Chủ tịch', '0988158427', '23887'),
('SIU Y BÉ', 'Chủ tịch', '0977370988', '23942'),
('TRẦN MINH TRIỀU', 'Chủ tịch', '0905335151', '23941'),
('TRẦN THANH HẢI', 'Chủ tịch', '0905725277', '23629'),
('HUỲNH SIỂM', 'Chủ tịch', '02693502456', '23677'),
('BÙI THỊ THƯƠNG', 'Chủ tịch', '0988130575', '23995'),
('MÃ VĂN TÌNH', 'Chủ tịch', '0986783147', '23644'),
('ĐINH ƠNG', 'Chủ tịch', '0976144303', '23683'),
('NGUYỄN CHÍ THANH', 'Chủ tịch', '0984791717', '23842'),
('NGUYỄN VĂN TUYẾN', 'Chủ tịch', '0853135612', '23857'),
('NGUYỄN ĐĂNG QUANG', 'Chủ tịch', '0905725277', '23611'),
('VÕ MINH QUANG', 'Chủ tịch', '0905823779', '23799'),
('NGUYỄN TRƯỜNG SINH', 'Chủ tịch', '0965221166', '23710'),
('NGUYỄN VĂN ÂN', 'Chủ tịch', '0913458108', '23911'),
('NGUYỄN CÔNG HOÀ', 'Chủ tịch', '0987754866', '23788'),
('NGUYỄN NGỌC NAM', 'Chủ tịch', '0968100679', '23869'),
('LÊ TRỌNG PHÚC', 'Chủ tịch', '0949145779', '23872'),
('ĐỖ VĂN ĐÔNG', 'Chủ tịch', '0914268715', '23764'),
('VÕ THUÝ VÂN', 'Chủ tịch', '02693607155', '24100'),
('VÕ HOÀNG LAN', 'Chủ tịch', '0985414155', '24061'),
('TRẦN MINH TÂM', 'Chủ tịch', '0979696026', '23971'),
('KRUNG DAM ĐOÀN', 'Chủ tịch', '0965751122', '23767'),
('PHẠM MINH CHÂU', 'Chủ tịch', '0982239994', '23728'),
('VÕ CÔNG HOÀ', 'Chủ tịch', '0905278779', '23977'),
('NGUYỄN THANH PHƯƠNG', 'Chủ tịch', '0989277394', '23776'),
('NGUYỄN QUỐC TƯ', 'Chủ tịch', '0799441982', '23866'),
('BÙI VĂN PHỤNG', 'Chủ tịch', '0979148685', '23935'),
('LƯU XUÂN THÀNH', 'Chủ tịch', '0989017779', '23986'),
('NGUYỄN TIẾN DŨNG', 'Chủ tịch', '0983114257', '23734'),
('TRẦN QUYẾT THẮNG', 'Chủ tịch', '0983447098', '23938'),
('NGUYỄN TRỌNG QUỲNH', 'Chủ tịch', '0989621446', '23884'),
('PHAN ĐÌNH THẮM', 'Chủ tịch', '0914317963', '23782'),
('LÊ TIẾN MẠNH', 'Chủ tịch', '0986974662', '24022'),
('NGUYỄN CÔNG SƠN', 'Chủ tịch', '0908590171', '23749'),
('NGUYỄN KIÊN CƯỜNG', 'Chủ tịch', '0983456631', '23926'),
('NGUYỄN HUỆ', 'Chủ tịch', '0905889949', '23881'),
('PHẠM THỊ THU HẰNG', 'Chủ tịch', '0982568459', '23917'),
('NGUYỄN CHÍ CƯỜNG', 'Chủ tịch', '0984894838', '24065'),
('VÕ NGỌC CHÂU', 'Chủ tịch', '0987409698', '24112'),
('KSOR H’ KHUYÊN', 'Chủ tịch', '0905079777', '24073'),
('NGUYỄN VĂN LUYẾN', 'Chủ tịch', '0988859040', '23908'),
('PHẠM VĂN ĐỨC', 'Chủ tịch', '0983652778', '24028'),
('NGUYỄN MẠNH CƯỜNG', 'Chủ tịch', '0975334413', '23638'),
('LƯƠNG NAM XUẤT THẾ', 'Chủ tịch', '0907904647', '23714'),
('VÕ NGỌC TUẤN', 'Chủ tịch', '0983225680', '23818'),
('NGUYỄN VĂN TRUNG', 'Chủ tịch', '0982000104', '23701'),
('TRẦN TRUNG TRỰC', 'Chủ tịch', '0328753700', '23674'),
('ĐẶNG THẾ QUYỀN', 'Chủ tịch', '0977897070', '23824'),
('LÊ DUY KIÊN', 'Chủ tịch', '0986387347', '23650'),
('LÊ LỢI', 'Chủ tịch', '0982221489', '23812'),
('VÕ LÊ XUÂN THIỆN', 'Chủ tịch', '0982346777', '23794'),
('NGUYỄN ANH TUẤN', 'Chủ tịch', '0914070090', '24043'),
('ĐẶNG HOÀI CHÂU', 'Chủ tịch', '0813090977', '24076'),
('TRẦN VĂN HÙNG', 'Chủ tịch', '0988963258', '24013'),
('NGUYỄN MẠNH TUYỂN', 'Chủ tịch', '0986284950', '23647'),
('NGUYỄN NGỌC SƠN', 'Chủ tịch', '0905201389', '23839'),
('LÊ THANH SƠN', 'Chủ tịch', '0983533158', '23668'),
('NGUYỄN BẢY THÀNH', 'Chủ tịch', '0975673067', '24109'),
('CAO NHẬT LAM', 'Chủ tịch', '0967330555', '24007'),
('TRỊNH MINH DƯƠNG', 'Chủ tịch', '0979373443', '23833'),
('NGUYỄN THỊ CHÂU', 'Chủ tịch', '0914234525', '23614'),
('ĐẶNG QUỐC HOÀI HUY', 'Chủ tịch', '0906548856', '23617');