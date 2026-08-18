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
