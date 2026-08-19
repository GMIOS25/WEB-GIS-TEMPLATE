export const FEATURE_FLAGS = {
  ocop: import.meta.env.VITE_ENABLE_OCOP === 'true',
  science: import.meta.env.VITE_ENABLE_SCIENCE === 'true',
  agriculture: import.meta.env.VITE_ENABLE_AGRICULTURE === 'true',
} as const;

export type FeatureFlagKey = keyof typeof FEATURE_FLAGS;

export const isFeatureEnabled = (key: FeatureFlagKey): boolean => {
  return FEATURE_FLAGS[key];
};
