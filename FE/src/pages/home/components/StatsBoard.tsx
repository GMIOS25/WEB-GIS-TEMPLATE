import React, { useMemo } from 'react';

export interface GeoJsonFeature {
  type: string;
  properties: {
    code: string;
    name: string;
    fullName?: string;
    areaKm2?: string | number;
  };
  geometry: {
    type: string;
    coordinates: number[][][] | number[][][][];
  };
}

export interface GeoJsonData {
  type: string;
  features: GeoJsonFeature[];
}

interface StatsBoardProps {
  geoJsonData: GeoJsonData | null;
}

const StatsBoard: React.FC<StatsBoardProps> = ({ geoJsonData }) => {
  const totalWards = useMemo(() => {
    return geoJsonData ? geoJsonData.features.length : 0;
  }, [geoJsonData]);

  const totalArea = useMemo(() => {
    return geoJsonData
      ? geoJsonData.features.reduce((sum: number, f) => sum + (Number(f.properties.areaKm2) || 0), 0)
      : 0;
  }, [geoJsonData]);

  return (
    <div className="absolute bottom-6 right-6 z-30 bg-white border border-neutral-200 rounded-2xl shadow-md p-4 w-[280px] hidden sm:block">
      <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-wider mb-3">Thống kê địa lý Gia Lai</h4>
      <div className="space-y-3">
        <div className="flex justify-between items-center text-sm border-b border-neutral-50 pb-2">
          <span className="text-neutral-500">Tổng xã/phường</span>
          <span className="font-bold text-neutral-900">{totalWards} xã/phường</span>
        </div>
        <div className="flex justify-between items-center text-sm pb-1">
          <span className="text-neutral-500">Tổng diện tích</span>
          <span className="font-bold text-neutral-900">
            {totalArea > 0 ? totalArea.toLocaleString('vi-VN', { maximumFractionDigits: 2 }) : '15.548,43'} km²
          </span>
        </div>
      </div>
    </div>
  );
};

export default React.memo(StatsBoard);
