import React, { useState, useEffect } from 'react';
import { X, FlaskConical, Upload, Loader2, Image as ImageIcon } from 'lucide-react';
import { createScienceUnit, updateScienceUnit } from '../../../api/science';
import { uploadFile } from '../../../api/files';
import api from '../../../api/axiosInstance';
import { extractErrorMessage } from '../../../api/errorUtils';
import type { ScienceUnit } from '../../../types/science';
import type { Ward } from '../../../types/gis';

interface ScienceFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (msg: string) => void;
  onError: (msg: string) => void;
  initialData?: ScienceUnit | null;
}

const ScienceFormModal: React.FC<ScienceFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onError,
  initialData,
}) => {
  const isEditing = !!initialData;
  const [name, setName] = useState(initialData?.name || '');
  const [unitType, setUnitType] = useState(initialData?.unitType || 'Trung tâm nghiên cứu');
  const [description, setDescription] = useState(initialData?.description || '');
  const [wardCode, setWardCode] = useState(initialData?.wardCode || '');
  const [latitude, setLatitude] = useState(initialData?.latitude?.toString() || '13.9850');
  const [longitude, setLongitude] = useState(initialData?.longitude?.toString() || '108.0150');
  const [imageUrl, setImageUrl] = useState(initialData?.imageUrl || '');
  const [wards, setWards] = useState<Ward[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);

  useEffect(() => {
    let active = true;
    api.get<Ward[]>('/api/wards')
      .then((res) => {
        if (active) {
          setWards(res.data);
          if (!initialData && res.data.length > 0) {
            setWardCode((prev) => prev || res.data[0].code);
          }
        }
      })
      .catch((err) => console.error('Failed to load wards', err));

    return () => {
      active = false;
    };
  }, [initialData]);

  if (!isOpen) return null;

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingImage(true);
    try {
      const stored = await uploadFile(file, 'science');
      setImageUrl(stored.publicUrl);
    } catch (err: unknown) {
      console.error(err);
      onError(extractErrorMessage(err, 'Không thể tải lên hình ảnh. Hãy chắc chắn file là JPEG/PNG dưới 5MB.'));
    } finally {
      setUploadingImage(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !wardCode) {
      onError('Vui lòng điền tên đơn vị và chọn xã/phường.');
      return;
    }

    const latNum = parseFloat(latitude);
    const lngNum = parseFloat(longitude);

    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      onError('Vĩ độ không hợp lệ (phải từ -90 đến 90).');
      return;
    }

    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      onError('Kinh độ không hợp lệ (phải từ -180 đến 180).');
      return;
    }

    setLoading(true);
    try {
      if (isEditing && initialData) {
        await updateScienceUnit(initialData.id, {
          name: name.trim(),
          unitType: unitType.trim(),
          description: description.trim(),
          wardCode,
          latitude: latNum,
          longitude: lngNum,
          imageUrl: imageUrl.trim() || undefined,
        });
        onSuccess(`Đã cập nhật đơn vị khoa học "${name}" thành công.`);
      } else {
        await createScienceUnit({
          name: name.trim(),
          unitType: unitType.trim(),
          description: description.trim(),
          wardCode,
          latitude: latNum,
          longitude: lngNum,
          imageUrl: imageUrl.trim() || undefined,
        });
        onSuccess(`Đã thêm đơn vị khoa học "${name}" thành công.`);
      }
      onClose();
    } catch (err: unknown) {
      console.error(err);
      onError(extractErrorMessage(err, 'Có lỗi xảy ra khi lưu đơn vị khoa học.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-lg w-full p-6 relative animate-zoomIn max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
          disabled={loading}
        >
          <X size={18} />
        </button>

        <div className="flex items-center space-x-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-slate-700 border border-slate-200">
            <FlaskConical size={20} />
          </div>
          <div>
            <h3 className="text-lg font-bold text-neutral-900">
              {isEditing ? 'Chỉnh sửa đơn vị khoa học' : 'Thêm đơn vị khoa học mới'}
            </h3>
            <p className="text-xs text-neutral-500">Quản lý cơ sở nghiên cứu và đơn vị khoa học công nghệ</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tên đơn vị <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Trung tâm Ứng dụng Tiến bộ KH&CN Gia Lai..."
              className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Loại hình
              </label>
              <select
                value={unitType}
                onChange={(e) => setUnitType(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all"
              >
                <option value="Trung tâm nghiên cứu">Trung tâm nghiên cứu</option>
                <option value="Phòng thí nghiệm">Phòng thí nghiệm</option>
                <option value="Trạm quan trắc">Trạm quan trắc</option>
                <option value="Cơ sở chuyển giao">Cơ sở chuyển giao</option>
                <option value="Viện nghiên cứu">Viện nghiên cứu</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Xã / Phường trực thuộc <span className="text-rose-500">*</span>
              </label>
              <select
                value={wardCode}
                onChange={(e) => setWardCode(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all"
                required
              >
                <option value="" disabled>-- Chọn xã/phường --</option>
                {wards.map((w) => (
                  <option key={w.code} value={w.code}>
                    {w.fullName || w.name} ({w.code})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Vĩ độ (Latitude) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="any"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                placeholder="13.9850"
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Kinh độ (Longitude) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="any"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                placeholder="108.0150"
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Mô tả hoạt động
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="Thông tin về chức năng nghiên cứu, đề tài KH&CN, công nghệ ứng dụng..."
              className="w-full px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500/20 focus:border-slate-500 transition-all resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Hình ảnh minh họa
            </label>
            <div className="flex items-center space-x-3">
              <label className="cursor-pointer py-2 px-3 bg-neutral-100 hover:bg-neutral-200 rounded-xl text-xs font-semibold text-neutral-700 flex items-center space-x-1.5 transition-colors border border-neutral-200">
                {uploadingImage ? (
                  <Loader2 size={14} className="animate-spin text-slate-700" />
                ) : (
                  <Upload size={14} />
                )}
                <span>{uploadingImage ? 'Đang tải lên...' : 'Chọn file ảnh (JPEG/PNG)'}</span>
                <input
                  type="file"
                  accept="image/png, image/jpeg, image/jpg"
                  onChange={handleImageUpload}
                  className="hidden"
                  disabled={uploadingImage || loading}
                />
              </label>
              {imageUrl && (
                <div className="flex items-center space-x-2 text-xs text-neutral-600 bg-slate-100 border border-slate-200 px-2.5 py-1.5 rounded-lg">
                  <ImageIcon size={14} className="text-slate-700" />
                  <span className="truncate max-w-[180px]">{imageUrl}</span>
                  <button
                    type="button"
                    onClick={() => setImageUrl('')}
                    className="text-neutral-400 hover:text-rose-500"
                  >
                    <X size={12} />
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="pt-4 flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-neutral-200 text-xs font-semibold text-neutral-600 hover:bg-neutral-50 transition-colors cursor-pointer"
              disabled={loading}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-slate-700 hover:bg-slate-800 text-xs font-semibold text-white transition-all shadow-sm shadow-slate-700/20 cursor-pointer flex items-center space-x-1.5"
              disabled={loading || uploadingImage}
            >
              {loading && <Loader2 size={14} className="animate-spin" />}
              <span>{isEditing ? 'Lưu thay đổi' : 'Tạo đơn vị'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ScienceFormModal;
