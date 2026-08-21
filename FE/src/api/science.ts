import api from './axiosInstance';
import type { ScienceUnit, ScienceUnitCreateRequest, ScienceUnitUpdateRequest } from '../types/science';
import type { PaginatedResponse } from '../types/common';
import type { PoiGeoJsonData } from '../types/gis';

export interface ScienceQueryParams {
  page?: number;
  size?: number;
  wardCode?: string;
  sort?: string;
}

export const fetchScienceUnits = async (params?: ScienceQueryParams): Promise<PaginatedResponse<ScienceUnit>> => {
  const response = await api.get<PaginatedResponse<ScienceUnit>>('/api/science', { params });
  return response.data;
};

export const fetchScienceGeoJson = async (): Promise<PoiGeoJsonData> => {
  const response = await api.get<PoiGeoJsonData>('/api/science/geojson');
  return response.data;
};

export const fetchScienceNearby = async (lat: number, lng: number, radiusKm: number): Promise<ScienceUnit[]> => {
  const response = await api.get<ScienceUnit[]>('/api/science/nearby', {
    params: { lat, lng, radiusKm },
  });
  return response.data;
};

export const fetchScienceUnitById = async (id: number): Promise<ScienceUnit> => {
  const response = await api.get<ScienceUnit>(`/api/science/${id}`);
  return response.data;
};

export const createScienceUnit = async (data: ScienceUnitCreateRequest): Promise<ScienceUnit> => {
  const response = await api.post<ScienceUnit>('/api/science', data);
  return response.data;
};

export const updateScienceUnit = async (id: number, data: ScienceUnitUpdateRequest): Promise<ScienceUnit> => {
  const response = await api.put<ScienceUnit>(`/api/science/${id}`, data);
  return response.data;
};

export const deleteScienceUnit = async (id: number): Promise<{ message: string }> => {
  const response = await api.delete<{ message: string }>(`/api/science/${id}`);
  return response.data;
};
