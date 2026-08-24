export interface LocalLeader {
  id?: number;
  fullName: string;
  position: string;
  phoneNumber?: string;
}

export interface Ward {
  code: string;
  name: string;
  fullName?: string;
  provinceName?: string;
  areaKm2?: number;
}

export interface WardDetail extends Ward {
  leaders?: LocalLeader[];
}

export interface GeoJsonFeature {
  type: string;
  properties: {
    code: string;
    name: string;
    fullName?: string;
    areaKm2?: string | number;
  };
  geometry: {
    type: string;
    coordinates: number[][][] | number[][][][];
  };
}

export interface GeoJsonData {
  type: string;
  features: GeoJsonFeature[];
}

export interface PoiGeoJsonProperties {
  id: number;
  name: string;
  productType?: string | null;
  productTypes?: string[] | null;
  starRating?: number | null;
  contactPhone?: string | null;
  locationAddress?: string | null;
  unitType?: string | null;
  wardCode?: string | null;
  wardName?: string | null;
  imageUrl?: string | null;
}

export interface PoiGeoJsonFeature {
  type: 'Feature';
  geometry: {
    type: 'Point';
    coordinates: [number, number]; // [lng, lat]
  };
  properties: PoiGeoJsonProperties;
}

export interface PoiGeoJsonData {
  type: 'FeatureCollection';
  features: PoiGeoJsonFeature[];
}
