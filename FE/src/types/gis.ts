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
