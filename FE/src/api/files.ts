import api from './axiosInstance';
import type { StoredFile } from '../types/common';

export const uploadFile = async (file: File, folder?: string): Promise<StoredFile> => {
  const formData = new FormData();
  formData.append('file', file);
  if (folder) {
    formData.append('folder', folder);
  }

  const response = await api.post<StoredFile>('/api/files', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};
