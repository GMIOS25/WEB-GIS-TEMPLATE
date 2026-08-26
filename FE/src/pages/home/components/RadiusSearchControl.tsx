import React, { useState, useEffect, useMemo } from 'react';
import { Radar, X, Search, MapPin, Loader2, Sparkles, FlaskConical, Trees } from 'lucide-react';
import { FEATURE_FLAGS } from '../../../config/features';
import { fetchOcopNearby } from '../../../api/ocop';
import { fetchScienceNearby } from '../../../api/science';
import { fetchAgricultureNearby } from '../../../api/agriculture';
import { extractErrorMessage } from '../../../api/errorUtils';

export interface RadiusSearchState {
  center: [number, number] | null;
  radiusKm: number;
  module: 'ocop' | 'science' | 'agriculture';
  resultIds: number[];
}

interface ModuleOption {
  id: 'ocop' | 'science' | 'agriculture';
  label: string;
  icon: React.ReactNode;
  color: string;
}

interface RadiusSearchControlProps {
  radiusSearchState: RadiusSearchState;
  setRadiusSearchState: React.Dispatch<React.SetStateAction<RadiusSearchState>>;
  isPickingCenter: boolean;
  setIsPickingCenter: (picking: boolean) => void;
  selectedPoiId?: number | null;
  onSelectDetail?: (type: 'ocop' | 'science' | 'agriculture', id: number) => void;
}

export const RadiusSearchControl: React.FC<RadiusSearchControlProps> = ({
  radiusSearchState,
  setRadiusSearchState,
  isPickingCenter,
  setIsPickingCenter,
  selectedPoiId = null,
  onSelectDetail,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<Array<{ id: number; name: string; wardCode?: string; wardName?: string }>>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const availableModules = useMemo<ModuleOption[]>(() => {
    const modules: ModuleOption[] = [];
    if (FEATURE_FLAGS.ocop) {
      modules.push({ id: 'ocop', label: 'Sản phẩm OCOP', icon: <Sparkles size={14} />, color: '#F97316' });
    }
    if (FEATURE_FLAGS.science) {
      modules.push({ id: 'science', label: 'Khoa học & CN', icon: <FlaskConical size={14} />, color: '#64748B' });
    }
    if (FEATURE_FLAGS.agriculture) {
      modules.push({ id: 'agriculture', label: 'Nông nghiệp & Trang trại', icon: <Trees size={14} />, color: '#6B7280' });
    }
    return modules;
  }, []);

  // Ensure selected module is enabled
  useEffect(() => {
    if (availableModules.length > 0 && !availableModules.some((m) => m.id === radiusSearchState.module)) {
      setRadiusSearchState((prev) => ({ ...prev, module: availableModules[0].id }));
    }
  }, [availableModules, radiusSearchState.module, setRadiusSearchState]);

  const handleExecuteSearch = async () => {
    if (!radiusSearchState.center) {
      setErrorMsg('Vui lòng chọn tâm điểm trên bản đồ trước khi tìm.');
      return;
    }

    const [lat, lng] = radiusSearchState.center;
    const radius = radiusSearchState.radiusKm;

    setLoading(true);
    setErrorMsg(null);
    try {
      let data: Array<{ id: number; name: string; wardCode?: string; wardName?: string }> = [];
      if (radiusSearchState.module === 'ocop') {
        data = await fetchOcopNearby(lat, lng, radius);
      } else if (radiusSearchState.module === 'science') {
        data = await fetchScienceNearby(lat, lng, radius);
      } else if (radiusSearchState.module === 'agriculture') {
        data = await fetchAgricultureNearby(lat, lng, radius);
      }

      setResults(data);
      setRadiusSearchState((prev) => ({
        ...prev,
        resultIds: data.map((d) => d.id),
      }));
    } catch (err: unknown) {
      console.error(err);
      setErrorMsg(extractErrorMessage(err, 'Lỗi khi tìm kiếm theo bán kính.'));
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setRadiusSearchState((prev) => ({
      ...prev,
      center: null,
      resultIds: [],
    }));
    setResults([]);
    setErrorMsg(null);
    setIsPickingCenter(false);
  };

  if (availableModules.length === 0) return null;

  return (
    <div className="absolute top-6 left-[380px] z-10 flex flex-col items-start space-y-2 select-none">
      {/* Trigger Button */}
      {!isOpen ? (
        <button
          onClick={() => setIsOpen(true)}
          className="bg-white/95 backdrop-blur-md px-4 py-2.5 rounded-2xl shadow-md border border-neutral-200/80 text-xs font-bold text-neutral-800 hover:text-emerald-600 hover:bg-white flex items-center space-x-2 transition-all cursor-pointer"
        >
          <Radar size={16} className="text-emerald-600 animate-pulse" />
          <span>Tìm kiếm theo bán kính</span>
        </button>
      ) : (
        /* Expanded Search Box */
        <div className="bg-white/95 backdrop-blur-md p-4 rounded-2xl shadow-xl border border-neutral-200 w-[320px] space-y-3.5 animate-fadeIn">
          <div className="flex items-center justify-between border-b border-neutral-100 pb-2">
            <div className="flex items-center space-x-2">
              <Radar size={16} className="text-emerald-600" />
              <span className="text-xs font-bold text-neutral-900">Tìm kiếm không gian (Radius)</span>
            </div>
            <button
              onClick={() => {
                setIsOpen(false);
                setIsPickingCenter(false);
              }}
              className="text-neutral-400 hover:text-neutral-600 cursor-pointer"
            >
              <X size={16} />
            </button>
          </div>

          {/* Module Selector */}
          <div>
            <label className="block text-[11px] font-semibold text-neutral-600 mb-1.5">Chuyên đề tìm kiếm:</label>
            <div className="grid grid-cols-1 gap-1.5">
              {availableModules.map((mod) => (
                <button
                  key={mod.id}
                  type="button"
                  onClick={() => {
                    setRadiusSearchState((prev) => ({ ...prev, module: mod.id, resultIds: [] }));
                    setResults([]);
                  }}
                  className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-2 border transition-all cursor-pointer ${
                    radiusSearchState.module === mod.id
                      ? 'bg-emerald-50 border-emerald-500 text-emerald-800 shadow-xs'
                      : 'bg-neutral-50 border-neutral-200 text-neutral-600 hover:bg-neutral-100'
                  }`}
                >
                  <span style={{ color: mod.color }}>{mod.icon}</span>
                  <span>{mod.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Center Point Picker */}
          <div>
            <label className="block text-[11px] font-semibold text-neutral-600 mb-1.5">Tâm điểm tìm kiếm:</label>
            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={() => setIsPickingCenter(!isPickingCenter)}
                className={`flex-1 py-1.5 px-3 rounded-lg text-xs font-semibold flex items-center justify-center space-x-1.5 border transition-colors cursor-pointer ${
                  isPickingCenter
                    ? 'bg-emerald-600 text-white border-emerald-600 animate-pulse'
                    : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700 border-neutral-200'
                }`}
              >
                <MapPin size={14} />
                <span>{isPickingCenter ? 'Click vào bản đồ để gán tâm...' : 'Chọn tâm trên bản đồ'}</span>
              </button>

              {radiusSearchState.center && (
                <button
                  type="button"
                  onClick={handleClear}
                  title="Xóa tâm điểm"
                  className="p-1.5 text-neutral-400 hover:text-rose-500 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                >
                  <X size={14} />
                </button>
              )}
            </div>
            {radiusSearchState.center && (
              <div className="mt-1 text-[11px] font-mono text-neutral-500 flex justify-between">
                <span>Vĩ độ: {radiusSearchState.center[0].toFixed(4)}</span>
                <span>Kinh độ: {radiusSearchState.center[1].toFixed(4)}</span>
              </div>
            )}
          </div>

          {/* Radius Selector */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-[11px] font-semibold text-neutral-600">Bán kính quét:</label>
              <span className="text-xs font-bold text-emerald-700">{radiusSearchState.radiusKm} km</span>
            </div>
            <input
              type="range"
              min="1"
              max="100"
              step="1"
              value={radiusSearchState.radiusKm}
              onChange={(e) =>
                setRadiusSearchState((prev) => ({
                  ...prev,
                  radiusKm: Number(e.target.value),
                }))
              }
              className="w-full h-1.5 bg-neutral-200 rounded-lg appearance-none cursor-pointer accent-emerald-600"
            />
            <div className="flex justify-between text-[10px] text-neutral-400 mt-1">
              <span>1 km</span>
              <span>25 km</span>
              <span>50 km</span>
              <span>100 km</span>
            </div>
          </div>

          {errorMsg && (
            <div className="text-rose-600 text-xs bg-rose-50 p-2 rounded-lg border border-rose-200">
              {errorMsg}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center space-x-2 pt-1">
            <button
              type="button"
              disabled={loading || !radiusSearchState.center}
              onClick={handleExecuteSearch}
              className="flex-1 py-2 px-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-600/20 flex items-center justify-center space-x-1.5 transition-all cursor-pointer"
            >
              {loading ? <Loader2 size={14} className="animate-spin" /> : <Search size={14} />}
              <span>Tìm kiếm</span>
            </button>
            {results.length > 0 && (
              <button
                type="button"
                onClick={handleClear}
                className="py-2 px-3 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-xl text-xs font-semibold transition-colors cursor-pointer"
              >
                Đặt lại
              </button>
            )}
          </div>

          {/* Result List */}
          {results.length > 0 && (
            <div className="border-t border-neutral-100 pt-2 space-y-1.5">
              <div className="text-[11px] font-bold text-neutral-700 flex justify-between">
                <span>Kết quả ({results.length}):</span>
                <span className="text-emerald-600 text-[10px]">Đang sáng trên bản đồ</span>
              </div>
              <div className="max-h-[140px] overflow-y-auto space-y-1 pr-1">
                {results.map((r) => {
                  const isSelected = selectedPoiId === r.id;
                  return (
                    <div
                      key={r.id}
                      onClick={() => onSelectDetail && onSelectDetail(radiusSearchState.module, r.id)}
                      className={`p-2 rounded-lg border text-xs cursor-pointer transition-all flex items-center justify-between ${
                        isSelected
                          ? 'bg-emerald-50 border-emerald-500 shadow-xs ring-1 ring-emerald-500'
                          : 'bg-neutral-50 hover:bg-emerald-50/60 border-neutral-200 hover:border-emerald-300'
                      }`}
                    >
                      <div className="min-w-0 pr-1.5 flex-1">
                        <div className={`truncate ${isSelected ? 'font-bold text-emerald-900' : 'font-semibold text-neutral-800'}`}>
                          {r.name}
                        </div>
                        {r.wardCode && (
                          <div className="text-[10px] text-neutral-500">Mã xã: {r.wardCode}</div>
                        )}
                      </div>
                      {isSelected && (
                        <span className="w-2 h-2 rounded-full bg-emerald-600 animate-ping shrink-0" />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
export default RadiusSearchControl;
