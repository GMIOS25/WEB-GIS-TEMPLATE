export const queryKeys = {
  wards: {
    all: ['wards'] as const,
    list: () => [...queryKeys.wards.all, 'list'] as const,
    detail: (code: string) => [...queryKeys.wards.all, 'detail', code] as const,
    geojson: () => [...queryKeys.wards.all, 'geojson'] as const,
    provinceGeojson: () => [...queryKeys.wards.all, 'provinceGeojson'] as const,
  },
  users: {
    all: ['users'] as const,
    list: () => [...queryKeys.users.all, 'list'] as const,
  },
  ocop: {
    all: ['ocop'] as const,
    list: (params?: { page?: number; size?: number; wardCode?: string }) =>
      [...queryKeys.ocop.all, 'list', params] as const,
    detail: (id: number) => [...queryKeys.ocop.all, 'detail', id] as const,
  },
  science: {
    all: ['science'] as const,
    list: (params?: { page?: number; size?: number; wardCode?: string }) =>
      [...queryKeys.science.all, 'list', params] as const,
    detail: (id: number) => [...queryKeys.science.all, 'detail', id] as const,
  },
  agriculture: {
    all: ['agriculture'] as const,
    list: (params?: { page?: number; size?: number; wardCode?: string }) =>
      [...queryKeys.agriculture.all, 'list', params] as const,
    detail: (id: number) => [...queryKeys.agriculture.all, 'detail', id] as const,
  },
};
