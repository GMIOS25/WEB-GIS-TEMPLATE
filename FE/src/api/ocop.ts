import api from './axiosInstance';
import type { OcopProduct, OcopProductCreateRequest, OcopProductUpdateRequest } from '../types/ocop';
import type { PaginatedResponse } from '../types/common';
import type { PoiGeoJsonData } from '../types/gis';

export interface OcopQueryParams {
  page?: number;
  size?: number;
  wardCode?: string;
  sort?: string;
}

export const fetchOcopProducts = async (params?: OcopQueryParams): Promise<PaginatedResponse<OcopProduct>> => {
  const response = await api.get<PaginatedResponse<OcopProduct>>('/api/ocop', { params });
  return response.data;
};

export const fetchOcopGeoJson = async (): Promise<PoiGeoJsonData> => {
  const response = await api.get<PoiGeoJsonData>('/api/ocop/geojson');
  return response.data;
};

export const fetchOcopNearby = async (lat: number, lng: number, radiusKm: number): Promise<OcopProduct[]> => {
  const response = await api.get<OcopProduct[]>('/api/ocop/nearby', {
    params: { lat, lng, radiusKm },
  });
  return response.data;
};

export const fetchOcopProductById = async (id: number): Promise<OcopProduct> => {
  const response = await api.get<OcopProduct>(`/api/ocop/${id}`);
  return response.data;
};

export const createOcopProduct = async (data: OcopProductCreateRequest): Promise<OcopProduct> => {
  const response = await api.post<OcopProduct>('/api/ocop', data);
  return response.data;
};

export const updateOcopProduct = async (id: number, data: OcopProductUpdateRequest): Promise<OcopProduct> => {
  const response = await api.put<OcopProduct>(`/api/ocop/${id}`, data);
  return response.data;
};

export const deleteOcopProduct = async (id: number): Promise<{ message: string }> => {
  const response = await api.delete<{ message: string }>(`/api/ocop/${id}`);
  return response.data;
};
