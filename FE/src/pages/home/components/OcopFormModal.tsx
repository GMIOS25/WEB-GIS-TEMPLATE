import React, { useState, useEffect } from 'react';
import { X, Sparkles, Upload, Loader2, Image as ImageIcon } from 'lucide-react';
import { createOcopProduct, updateOcopProduct } from '../../../api/ocop';
import { uploadFile } from '../../../api/files';
import api from '../../../api/axiosInstance';
import { extractErrorMessage } from '../../../api/errorUtils';
import type { OcopProduct } from '../../../types/ocop';
import type { Ward } from '../../../types/gis';

interface OcopFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (msg: string) => void;
  onError: (msg: string) => void;
  initialData?: OcopProduct | null;
}

const OcopFormModal: React.FC<OcopFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onError,
  initialData,
}) => {
  const isEditing = !!initialData;
  const [name, setName] = useState(initialData?.name || '');
  const [productType, setProductType] = useState(initialData?.productType || 'Nông sản');
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
      const stored = await uploadFile(file, 'ocop');
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
      onError('Vui lòng điền tên sản phẩm và chọn xã/phường.');
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
        await updateOcopProduct(initialData.id, {
          name: name.trim(),
          productType: productType.trim(),
          description: description.trim(),
          wardCode,
          latitude: latNum,
          longitude: lngNum,
          imageUrl: imageUrl.trim() || undefined,
        });
        onSuccess(`Đã cập nhật sản phẩm OCOP "${name}" thành công.`);
      } else {
        await createOcopProduct({
          name: name.trim(),
          productType: productType.trim(),
          description: description.trim(),
          wardCode,
          latitude: latNum,
          longitude: lngNum,
          imageUrl: imageUrl.trim() || undefined,
        });
        onSuccess(`Đã thêm sản phẩm OCOP "${name}" thành công.`);
      }
      onClose();
    } catch (err: unknown) {
      console.error(err);
      onError(extractErrorMessage(err, 'Có lỗi xảy ra khi lưu sản phẩm OCOP.'));
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
          <div className="w-10 h-10 rounded-xl bg-orange-50 flex items-center justify-center text-orange-600 border border-orange-100">
            <Sparkles size={20} />
          </div>
          <div>
            <h3 className="text-lg font-bold text-neutral-900">
              {isEditing ? 'Chỉnh sửa sản phẩm OCOP' : 'Thêm sản phẩm OCOP mới'}
            </h3>
            <p className="text-xs text-neutral-500">Chương trình Mỗi xã một sản phẩm (OCOP Gia Lai)</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tên sản phẩm <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Cà phê Pleiku, Tiêu Đắk Đoa, Bò một nắng..."
              className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Phân loại
              </label>
              <select
                value={productType}
                onChange={(e) => setProductType(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all"
              >
                <option value="Nông sản">Nông sản</option>
                <option value="Thực phẩm">Thực phẩm</option>
                <option value="Đồ uống">Đồ uống</option>
                <option value="Thảo dược">Thảo dược</option>
                <option value="Thủ công mỹ nghệ">Thủ công mỹ nghệ</option>
                <option value="Dịch vụ du lịch">Dịch vụ du lịch</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Xã / Phường trực thuộc <span className="text-rose-500">*</span>
              </label>
              <select
                value={wardCode}
                onChange={(e) => setWardCode(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all"
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
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all"
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
                className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Mô tả chi tiết
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="Thông tin giới thiệu về sản phẩm OCOP, xếp hạng sao, hợp tác xã..."
              className="w-full px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Hình ảnh minh họa
            </label>
            <div className="flex items-center space-x-3">
              <label className="cursor-pointer py-2 px-3 bg-neutral-100 hover:bg-neutral-200 rounded-xl text-xs font-semibold text-neutral-700 flex items-center space-x-1.5 transition-colors border border-neutral-200">
                {uploadingImage ? (
                  <Loader2 size={14} className="animate-spin text-orange-600" />
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
                <div className="flex items-center space-x-2 text-xs text-neutral-600 bg-orange-50 border border-orange-200 px-2.5 py-1.5 rounded-lg">
                  <ImageIcon size={14} className="text-orange-600" />
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
              className="px-5 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-xs font-semibold text-white transition-all shadow-sm shadow-orange-600/20 cursor-pointer flex items-center space-x-1.5"
              disabled={loading || uploadingImage}
            >
              {loading && <Loader2 size={14} className="animate-spin" />}
              <span>{isEditing ? 'Lưu thay đổi' : 'Tạo sản phẩm'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default OcopFormModal;
