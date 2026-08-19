export interface OcopProduct {
  id: number;
  name: string;
  productType?: string;
  description?: string;
  wardCode: string;
  wardName?: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface OcopProductCreateRequest {
  name: string;
  productType?: string;
  description?: string;
  wardCode: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface OcopProductUpdateRequest {
  name?: string;
  productType?: string;
  description?: string;
  wardCode?: string;
  latitude?: number;
  longitude?: number;
  imageUrl?: string;
}
