import api from './axiosInstance';
import type { AgricultureUnit, AgricultureUnitCreateRequest, AgricultureUnitUpdateRequest } from '../types/agriculture';
import type { PaginatedResponse } from '../types/common';

export interface AgricultureQueryParams {
  page?: number;
  size?: number;
  wardCode?: string;
  sort?: string;
}

export const fetchAgricultureUnits = async (params?: AgricultureQueryParams): Promise<PaginatedResponse<AgricultureUnit>> => {
  const response = await api.get<PaginatedResponse<AgricultureUnit>>('/api/agriculture', { params });
  return response.data;
};

export const fetchAgricultureUnitById = async (id: number): Promise<AgricultureUnit> => {
  const response = await api.get<AgricultureUnit>(`/api/agriculture/${id}`);
  return response.data;
};

export const createAgricultureUnit = async (data: AgricultureUnitCreateRequest): Promise<AgricultureUnit> => {
  const response = await api.post<AgricultureUnit>('/api/agriculture', data);
  return response.data;
};

export const updateAgricultureUnit = async (id: number, data: AgricultureUnitUpdateRequest): Promise<AgricultureUnit> => {
  const response = await api.put<AgricultureUnit>(`/api/agriculture/${id}`, data);
  return response.data;
};

export const deleteAgricultureUnit = async (id: number): Promise<{ message: string }> => {
  const response = await api.delete<{ message: string }>(`/api/agriculture/${id}`);
  return response.data;
};
