import React, { useMemo } from 'react';
import type { GeoJsonData } from '../../../types/gis';
import { FEATURE_FLAGS } from '../../../config/features';

interface StatsBoardProps {
  geoJsonData: GeoJsonData | null;
  ocopCount?: number;
  scienceCount?: number;
  agricultureCount?: number;
}

const StatsBoard: React.FC<StatsBoardProps> = ({
  geoJsonData,
  ocopCount = 0,
  scienceCount = 0,
  agricultureCount = 0,
}) => {
  const totalWards = useMemo(() => {
    return geoJsonData ? geoJsonData.features.length : 0;
  }, [geoJsonData]);

  const totalArea = useMemo(() => {
    return geoJsonData
      ? geoJsonData.features.reduce((sum: number, f) => sum + (Number(f.properties.areaKm2) || 0), 0)
      : 0;
  }, [geoJsonData]);

  return (
    <div className="absolute bottom-6 right-6 z-30 bg-white border border-neutral-200 rounded-2xl shadow-lg p-4 w-[300px] hidden sm:block select-none">
      <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-wider mb-2.5">
        Tổng quan dữ liệu GIS Gia Lai
      </h4>
      <div className="space-y-2">
        <div className="flex justify-between items-center text-xs border-b border-neutral-100 pb-1.5">
          <span className="text-neutral-500">Tổng số xã/phường</span>
          <span className="font-bold text-neutral-900">{totalWards} đơn vị</span>
        </div>

        <div className="flex justify-between items-center text-xs border-b border-neutral-100 pb-1.5">
          <span className="text-neutral-500">Tổng diện tích tự nhiên</span>
          <span className="font-bold text-neutral-900">
            {totalArea > 0
              ? `${totalArea.toLocaleString('vi-VN', { maximumFractionDigits: 2 })} km²`
              : 'Đang tải...'}
          </span>
        </div>

        {FEATURE_FLAGS.ocop && (
          <div className="flex justify-between items-center text-xs border-b border-neutral-100 pb-1.5">
            <span className="text-neutral-500 flex items-center space-x-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#F97316]"></span>
              <span>Sản phẩm OCOP</span>
            </span>
            <span className="font-bold text-orange-600">{ocopCount} sản phẩm</span>
          </div>
        )}

        {FEATURE_FLAGS.science && (
          <div className="flex justify-between items-center text-xs border-b border-neutral-100 pb-1.5">
            <span className="text-neutral-500 flex items-center space-x-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#64748B]"></span>
              <span>Đơn vị KH&CN</span>
            </span>
            <span className="font-bold text-slate-700">{scienceCount} đơn vị</span>
          </div>
        )}

        {FEATURE_FLAGS.agriculture && (
          <div className="flex justify-between items-center text-xs pb-1">
            <span className="text-neutral-500 flex items-center space-x-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#6B7280]"></span>
              <span>Trang trại nông nghiệp</span>
            </span>
            <span className="font-bold text-neutral-700">{agricultureCount} trang trại</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default React.memo(StatsBoard);
