import React from 'react';
import { X, Sparkles, FlaskConical, Trees, MapPin, Tag, FileText } from 'lucide-react';
import type { GeoJsonFeature } from '../../../types/gis';

export interface SelectedPoiDetail {
  moduleType: 'ocop' | 'science' | 'agriculture';
  id: number;
  name: string;
  typeBadge?: string;
  description?: string;
  wardCode: string;
  wardName?: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

interface DetailsPanelProps {
  selectedWard: GeoJsonFeature | null;
  setSelectedWard: (ward: GeoJsonFeature | null) => void;
  selectedPoi: SelectedPoiDetail | null;
  setSelectedPoi: (poi: SelectedPoiDetail | null) => void;
}

const DetailsPanel: React.FC<DetailsPanelProps> = ({
  selectedWard,
  setSelectedWard,
  selectedPoi,
  setSelectedPoi,
}) => {
  if (!selectedWard && !selectedPoi) return null;

  // Render POI Detail
  if (selectedPoi) {
    const isOcop = selectedPoi.moduleType === 'ocop';
    const isScience = selectedPoi.moduleType === 'science';
    const isAgriculture = selectedPoi.moduleType === 'agriculture';

    const moduleTitle = isOcop
      ? 'Sản phẩm OCOP'
      : isScience
      ? 'Khoa học & Công nghệ'
      : 'Nông nghiệp & Trang trại';

    const badgeColor = isOcop
      ? 'bg-orange-50 text-orange-700 border-orange-200'
      : isScience
      ? 'bg-slate-50 text-slate-700 border-slate-200'
      : 'bg-neutral-100 text-neutral-700 border-neutral-200';

    return (
      <div className="absolute top-24 bottom-24 right-6 w-[360px] bg-white border border-neutral-200 rounded-2xl shadow-xl p-6 flex flex-col justify-between z-30 animate-slideLeft overflow-y-auto">
        <div className="space-y-4">
          <div className="flex justify-between items-start pb-3 border-b border-neutral-100">
            <div>
              <span className={`inline-flex items-center space-x-1 text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full border ${badgeColor}`}>
                {isOcop && <Sparkles size={11} />}
                {isScience && <FlaskConical size={11} />}
                {isAgriculture && <Trees size={11} />}
                <span>{moduleTitle}</span>
              </span>
              <h3 className="text-base font-bold text-neutral-900 mt-2 line-clamp-2">
                {selectedPoi.name}
              </h3>
            </div>
            <button
              onClick={() => setSelectedPoi(null)}
              className="p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 transition-all cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>

          {/* POI Image */}
          {selectedPoi.imageUrl && (
            <div className="rounded-xl overflow-hidden border border-neutral-200 shadow-xs h-[160px] bg-neutral-100 relative">
              <img
                src={selectedPoi.imageUrl}
                alt={selectedPoi.name}
                className="w-full h-full object-cover"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            </div>
          )}

          {/* Details Card */}
          <div className="space-y-2.5">
            {selectedPoi.typeBadge && (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <Tag size={15} className="text-neutral-500 mt-0.5" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Phân loại / Loại hình</p>
                  <p className="text-xs font-bold text-neutral-800">{selectedPoi.typeBadge}</p>
                </div>
              </div>
            )}

            <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
              <MapPin size={15} className="text-neutral-500 mt-0.5" />
              <div>
                <p className="text-[11px] text-neutral-400 font-medium">Địa bàn hành chính</p>
                <p className="text-xs font-bold text-neutral-800">
                  {selectedPoi.wardName || `Mã xã: ${selectedPoi.wardCode}`}
                </p>
                <p className="text-[10px] text-neutral-400 font-mono mt-0.5">
                  Tọa độ: {selectedPoi.latitude.toFixed(5)}, {selectedPoi.longitude.toFixed(5)}
                </p>
              </div>
            </div>

            {selectedPoi.description && (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <FileText size={15} className="text-neutral-500 mt-0.5" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Mô tả chi tiết</p>
                  <p className="text-xs text-neutral-700 whitespace-pre-line leading-relaxed mt-0.5">
                    {selectedPoi.description}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="pt-3 border-t border-neutral-100 text-center">
          <p className="text-[10px] text-neutral-400 font-medium">Hệ thống thông tin GIS tỉnh Gia Lai</p>
        </div>
      </div>
    );
  }

  // Render Ward Detail
  return (
    <div className="absolute top-24 bottom-24 right-6 w-[340px] bg-white border border-neutral-200 rounded-2xl shadow-lg p-6 flex flex-col justify-between z-30 animate-slideLeft">
      <div className="space-y-6">
        <div className="flex justify-between items-start pb-4 border-b border-neutral-100">
          <div>
            <span className="inline-block text-[9px] font-bold uppercase tracking-wider bg-primary-50 text-primary-700 px-2 py-0.5 rounded-full border border-primary-100">
              Chi tiết địa giới
            </span>
            <h3 className="text-lg font-bold text-neutral-900 mt-2">
              {selectedWard!.properties.fullName || selectedWard!.properties.name}
            </h3>
          </div>
          <button
            onClick={() => setSelectedWard(null)}
            className="p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 transition-all cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        <div className="space-y-4">
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1">
            <p className="text-xs text-neutral-400 font-medium">Mã hành chính</p>
            <p className="text-sm font-bold text-neutral-800">{selectedWard!.properties.code}</p>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1">
            <p className="text-xs text-neutral-400 font-medium">Tỉnh thành</p>
            <p className="text-sm font-bold text-neutral-800">Tỉnh Gia Lai (mã 52)</p>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1">
            <p className="text-xs text-neutral-400 font-medium">Diện tích xã/phường</p>
            <p className="text-sm font-bold text-neutral-800">
              {selectedWard!.properties.areaKm2 ? Number(selectedWard!.properties.areaKm2).toFixed(2) : '---'} km²
            </p>
          </div>
        </div>
      </div>

      <div className="pt-4 border-t border-neutral-100 text-center">
        <p className="text-[11px] text-neutral-400 font-medium">Bản đồ địa giới Gia Lai chính thức</p>
      </div>
    </div>
  );
};

export default React.memo(DetailsPanel);
