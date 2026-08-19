export interface ScienceUnit {
  id: number;
  name: string;
  unitType?: string;
  description?: string;
  wardCode: string;
  wardName?: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface ScienceUnitCreateRequest {
  name: string;
  unitType?: string;
  description?: string;
  wardCode: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface ScienceUnitUpdateRequest {
  name?: string;
  unitType?: string;
  description?: string;
  wardCode?: string;
  latitude?: number;
  longitude?: number;
  imageUrl?: string;
}
