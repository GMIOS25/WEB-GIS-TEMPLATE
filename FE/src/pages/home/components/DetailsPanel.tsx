import React from 'react';
import { X } from 'lucide-react';

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

interface DetailsPanelProps {
  selectedWard: GeoJsonFeature | null;
  setSelectedWard: (ward: GeoJsonFeature | null) => void;
}

const DetailsPanel: React.FC<DetailsPanelProps> = ({ selectedWard, setSelectedWard }) => {
  if (!selectedWard) return null;

  return (
    <div className="absolute top-24 bottom-24 right-6 w-[340px] bg-white border border-neutral-200 rounded-2xl shadow-lg p-6 flex flex-col justify-between z-30 animate-slideLeft">
      <div className="space-y-6">
        <div className="flex justify-between items-start pb-4 border-b border-neutral-100">
          <div>
            <span className="inline-block text-[9px] font-bold uppercase tracking-wider bg-primary-50 text-primary-700 px-2 py-0.5 rounded-full border border-primary-100">
              Chi tiết địa giới
            </span>
            <h3 className="text-lg font-bold text-neutral-900 mt-2">
              {selectedWard.properties.fullName || selectedWard.properties.name}
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
            <p className="text-sm font-bold text-neutral-800">{selectedWard.properties.code}</p>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1">
            <p className="text-xs text-neutral-400 font-medium">Tỉnh thành</p>
            <p className="text-sm font-bold text-neutral-800">Tỉnh Gia Lai (mã 52)</p>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1">
            <p className="text-xs text-neutral-400 font-medium">Diện tích xã/phường</p>
            <p className="text-sm font-bold text-neutral-800">
              {selectedWard.properties.areaKm2 ? Number(selectedWard.properties.areaKm2).toFixed(2) : '---'} km²
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
