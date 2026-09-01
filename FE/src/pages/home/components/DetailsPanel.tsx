import React from 'react';
import {
  X,
  Sparkles,
  FlaskConical,
  Trees,
  MapPin,
  Tag,
  FileText,
  Star,
  Phone,
  Home,
  UserCheck,
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '../../../api/axiosInstance';
import { queryKeys } from '../../../api/queryKeys';
import type { GeoJsonFeature, WardDetail } from '../../../types/gis';
import { resolveImageUrl } from '../../../utils/media';

export interface SelectedPoiDetail {
  moduleType: 'ocop' | 'science' | 'agriculture';
  id: number;
  name: string;
  typeBadge?: string;
  productTypes?: string[];
  starRating?: number;
  contactPhone?: string;
  locationAddress?: string;
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
  const wardCode = selectedWard?.properties.code;

  // Fetch full ward details including local leaders
  const { data: wardDetail, isLoading: isWardDetailLoading } = useQuery<WardDetail>({
    queryKey: queryKeys.wards.detail(wardCode || ''),
    queryFn: async () => {
      const res = await api.get<WardDetail>(`/api/wards/${wardCode}`);
      return res.data;
    },
    enabled: !!wardCode,
    staleTime: 1000 * 60 * 5, // 5 mins
  });

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
              <span
                className={`inline-flex items-center space-x-1 text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full border ${badgeColor}`}
              >
                {isOcop && <Sparkles size={11} />}
                {isScience && <FlaskConical size={11} />}
                {isAgriculture && <Trees size={11} />}
                <span>{moduleTitle}</span>
              </span>
              <h3 className="text-base font-bold text-neutral-900 mt-2 line-clamp-2">
                {selectedPoi.name}
              </h3>

              {/* Star Rating for OCOP */}
              {isOcop && selectedPoi.starRating && (
                <div className="flex items-center space-x-1 mt-1.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      size={14}
                      className={`${
                        star <= (selectedPoi.starRating || 0)
                          ? 'fill-amber-400 text-amber-500'
                          : 'fill-neutral-200 text-neutral-200'
                      }`}
                    />
                  ))}
                  <span className="text-xs font-bold text-amber-600 ml-1">
                    {selectedPoi.starRating} sao OCOP
                  </span>
                </div>
              )}
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
                src={resolveImageUrl(selectedPoi.imageUrl)}
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
            {/* Product Types or Single Type Badge */}
            {selectedPoi.productTypes && selectedPoi.productTypes.length > 0 ? (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <Tag size={15} className="text-orange-500 mt-0.5 shrink-0" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Ngành hàng</p>
                  <div className="flex flex-wrap gap-1 mt-1">
                    {selectedPoi.productTypes.map((t, idx) => (
                      <span
                        key={idx}
                        className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-semibold bg-orange-50 text-orange-700 border border-orange-200"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ) : selectedPoi.typeBadge ? (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <Tag size={15} className="text-neutral-500 mt-0.5 shrink-0" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Phân loại / Loại hình</p>
                  <p className="text-xs font-bold text-neutral-800">{selectedPoi.typeBadge}</p>
                </div>
              </div>
            ) : null}

            {/* Address */}
            {selectedPoi.locationAddress && (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <Home size={15} className="text-neutral-500 mt-0.5 shrink-0" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Địa chỉ cơ sở</p>
                  <p className="text-xs font-semibold text-neutral-800">
                    {selectedPoi.locationAddress}
                  </p>
                </div>
              </div>
            )}

            {/* Phone */}
            {selectedPoi.contactPhone && (
              <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
                <Phone size={15} className="text-emerald-600 mt-0.5 shrink-0" />
                <div>
                  <p className="text-[11px] text-neutral-400 font-medium">Điện thoại liên hệ</p>
                  <a
                    href={`tel:${selectedPoi.contactPhone}`}
                    className="text-xs font-bold text-emerald-700 hover:underline"
                  >
                    {selectedPoi.contactPhone}
                  </a>
                </div>
              </div>
            )}

            {/* Administrative boundary & Coordinates */}
            <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
              <MapPin size={15} className="text-neutral-500 mt-0.5 shrink-0" />
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
                <FileText size={15} className="text-neutral-500 mt-0.5 shrink-0" />
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
          <p className="text-[10px] text-neutral-400 font-medium">
            Hệ thống thông tin GIS tỉnh Gia Lai
          </p>
        </div>
      </div>
    );
  }

  // Render Ward Detail
  const leaders = wardDetail?.leaders || [];

  return (
    <div className="absolute top-24 bottom-24 right-6 w-[360px] bg-white border border-neutral-200 rounded-2xl shadow-xl p-6 flex flex-col justify-between z-30 animate-slideLeft overflow-y-auto">
      <div className="space-y-4">
        <div className="flex justify-between items-start pb-3 border-b border-neutral-100">
          <div>
            <span className="inline-block text-[9px] font-bold uppercase tracking-wider bg-primary-50 text-primary-700 px-2.5 py-0.5 rounded-full border border-primary-100">
              Chi tiết địa giới
            </span>
            <h3 className="text-base font-bold text-neutral-900 mt-2">
              {wardDetail?.fullName ||
                selectedWard!.properties.fullName ||
                selectedWard!.properties.name}
            </h3>
          </div>
          <button
            onClick={() => setSelectedWard(null)}
            className="p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 transition-all cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        <div className="space-y-2.5">
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
            <MapPin size={15} className="text-neutral-500 mt-0.5 shrink-0" />
            <div>
              <p className="text-[11px] text-neutral-400 font-medium">Mã hành chính</p>
              <p className="text-xs font-bold text-neutral-800">{selectedWard!.properties.code}</p>
            </div>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
            <Home size={15} className="text-neutral-500 mt-0.5 shrink-0" />
            <div>
              <p className="text-[11px] text-neutral-400 font-medium">Tỉnh thành</p>
              <p className="text-xs font-bold text-neutral-800">
                {wardDetail?.provinceName || 'Tỉnh Gia Lai (mã 52)'}
              </p>
            </div>
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-100 flex items-start space-x-2.5">
            <Tag size={15} className="text-neutral-500 mt-0.5 shrink-0" />
            <div>
              <p className="text-[11px] text-neutral-400 font-medium">Diện tích xã/phường</p>
              <p className="text-xs font-bold text-neutral-800">
                {(wardDetail?.areaKm2 ?? selectedWard!.properties.areaKm2)
                  ? `${Number(wardDetail?.areaKm2 ?? selectedWard!.properties.areaKm2).toFixed(2)} km²`
                  : '---'}
              </p>
            </div>
          </div>

          {/* Leadership Section */}
          <div className="pt-2">
            <div className="flex items-center space-x-1.5 mb-2 px-1">
              <UserCheck size={15} className="text-primary-600" />
              <h4 className="text-xs font-bold text-neutral-800 uppercase tracking-wider">
                Ban Lãnh đạo xã/phường
              </h4>
            </div>

            {isWardDetailLoading ? (
              <div className="p-4 text-center bg-neutral-50 rounded-xl border border-neutral-100">
                <div className="inline-block w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
                <p className="text-[11px] text-neutral-400 mt-1">Đang tải thông tin lãnh đạo...</p>
              </div>
            ) : leaders.length > 0 ? (
              <div className="space-y-2">
                {leaders.map((leader, idx) => (
                  <div
                    key={leader.id || idx}
                    className="p-3 bg-gradient-to-br from-primary-50/40 to-neutral-50 rounded-xl border border-primary-100/70 space-y-1.5"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-neutral-900">{leader.fullName}</span>
                      <span className="px-2 py-0.5 text-[10px] font-semibold bg-primary-100/80 text-primary-800 border border-primary-200 rounded-md">
                        {leader.position}
                      </span>
                    </div>
                    {leader.phoneNumber ? (
                      <div className="flex items-center space-x-1.5 text-xs text-neutral-600">
                        <Phone size={12} className="text-emerald-600 shrink-0" />
                        <a
                          href={`tel:${leader.phoneNumber}`}
                          className="font-semibold text-emerald-700 hover:underline font-mono text-[11px]"
                        >
                          {leader.phoneNumber}
                        </a>
                      </div>
                    ) : (
                      <p className="text-[10px] text-neutral-400 italic">Chưa cập nhật SĐT</p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-3.5 bg-neutral-50 rounded-xl border border-neutral-100 text-center">
                <p className="text-xs text-neutral-400 italic">Chưa có thông tin lãnh đạo</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="pt-3 border-t border-neutral-100 text-center mt-4">
        <p className="text-[10px] text-neutral-400 font-medium">
          Hệ thống thông tin GIS tỉnh Gia Lai
        </p>
      </div>
    </div>
  );
};

export default React.memo(DetailsPanel);
