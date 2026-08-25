export interface OcopProduct {
  id: number;
  name: string;
  productTypes?: string[];
  starRating?: number; // 1 - 5 sao
  contactPhone?: string;
  locationAddress?: string;
  wardCode: string;
  wardName?: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface OcopProductCreateRequest {
  name: string;
  productTypes?: string[];
  starRating?: number;
  contactPhone?: string;
  locationAddress?: string;
  wardCode: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface OcopProductUpdateRequest {
  name?: string;
  productTypes?: string[];
  starRating?: number;
  contactPhone?: string;
  locationAddress?: string;
  wardCode?: string;
  latitude?: number;
  longitude?: number;
  imageUrl?: string;
}
