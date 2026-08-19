export interface AgricultureUnit {
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

export interface AgricultureUnitCreateRequest {
  name: string;
  unitType?: string;
  description?: string;
  wardCode: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface AgricultureUnitUpdateRequest {
  name?: string;
  unitType?: string;
  description?: string;
  wardCode?: string;
  latitude?: number;
  longitude?: number;
  imageUrl?: string;
}
