import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { Sparkles, Plus, AlertCircle, Info, Edit2, Trash2, MapPin, ChevronLeft, ChevronRight, ArrowLeft, Loader2 } from 'lucide-react';
import { fetchOcopProducts, deleteOcopProduct } from '../../../api/ocop';
import api from '../../../api/axiosInstance';
import OcopFormModal from './OcopFormModal';
import { extractErrorMessage } from '../../../api/errorUtils';
import type { OcopProduct } from '../../../types/ocop';
import type { Ward } from '../../../types/gis';

interface OcopPanelProps {
  setActiveView: (view: 'map' | 'admin' | 'ocop' | 'science' | 'agriculture') => void;
}

const OcopPanel: React.FC<OcopPanelProps> = ({ setActiveView }) => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [products, setProducts] = useState<OcopProduct[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Pagination & Filter state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedWard, setSelectedWard] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [wards, setWards] = useState<Ward[]>([]);

  // Modals state
  const [showFormModal, setShowFormModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<OcopProduct | null>(null);
  const [deletingProduct, setDeletingProduct] = useState<OcopProduct | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Fetch wards list for filter
  useEffect(() => {
    let active = true;
    api.get<Ward[]>('/api/wards')
      .then((res) => {
        if (active) setWards(res.data);
      })
      .catch((err) => console.error('Failed to fetch wards', err));

    return () => {
      active = false;
    };
  }, []);

  // Fetch OCOP products on param change
  useEffect(() => {
    let active = true;
    fetchOcopProducts({
      page: currentPage,
      size: 10,
      wardCode: selectedWard || undefined,
    })
      .then((data) => {
        if (active) {
          setProducts(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          console.error(err);
          setError(extractErrorMessage(err, 'Không thể tải danh sách sản phẩm OCOP.'));
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [currentPage, selectedWard]);

  const loadProducts = useCallback(() => {
    setLoading(true);
    fetchOcopProducts({
      page: currentPage,
      size: 10,
      wardCode: selectedWard || undefined,
    })
      .then((data) => {
        setProducts(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        console.error(err);
        setError(extractErrorMessage(err, 'Không thể tải danh sách sản phẩm OCOP.'));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [currentPage, selectedWard]);

  const handleSuccess = (msg: string) => {
    setSuccess(msg);
    setError(null);
    loadProducts();
  };

  const handleError = (msg: string) => {
    setError(msg);
    setSuccess(null);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingProduct) return;
    setDeleteLoading(true);
    try {
      await deleteOcopProduct(deletingProduct.id);
      setSuccess(`Đã xóa sản phẩm "${deletingProduct.name}" thành công.`);
      setDeletingProduct(null);
      loadProducts();
    } catch (err: unknown) {
      console.error(err);
      setError(extractErrorMessage(err, 'Có lỗi xảy ra khi xóa sản phẩm.'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Filter client-side search query
  const filteredProducts = products.filter((p) =>
    searchTerm ? p.name.toLowerCase().includes(searchTerm.toLowerCase()) || p.productType?.toLowerCase().includes(searchTerm.toLowerCase()) : true
  );

  return (
    <div className="w-full h-full bg-neutral-50 overflow-y-auto z-20 flex flex-col p-6 sm:p-10 pt-24">
      <div className="w-full max-w-6xl mx-auto bg-white border border-neutral-200 rounded-2xl shadow-sm p-6 sm:p-8">
        
        {/* Header with Back button */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-4 border-b border-neutral-100">
          <div className="flex items-center space-x-3">
            <button
              onClick={() => setActiveView('map')}
              className="p-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-600 rounded-xl transition-colors cursor-pointer"
              title="Quay lại bản đồ"
            >
              <ArrowLeft size={18} />
            </button>
            <div className="w-10 h-10 rounded-xl bg-orange-50 flex items-center justify-center text-orange-600 border border-orange-100">
              <Sparkles size={20} />
            </div>
            <div>
              <h3 className="text-xl font-bold text-neutral-900">Quản lý Sản phẩm OCOP</h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                Chương trình Mỗi xã một sản phẩm tỉnh Gia Lai (Tổng số: {totalElements})
              </p>
            </div>
          </div>

          {isAdmin && (
            <button
              onClick={() => {
                setEditingProduct(null);
                setShowFormModal(true);
              }}
              className="py-2.5 px-4 rounded-xl bg-orange-600 hover:bg-orange-700 active:scale-[0.98] text-white text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-sm shadow-orange-600/20 cursor-pointer self-start sm:self-auto"
            >
              <Plus size={16} />
              <span>Thêm sản phẩm OCOP</span>
            </button>
          )}
        </div>

        {/* Filters and search */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
          <input
            type="text"
            placeholder="Tìm theo tên hoặc phân loại..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
          />
          <select
            value={selectedWard}
            onChange={(e) => {
              setSelectedWard(e.target.value);
              setCurrentPage(0);
            }}
            className="px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
          >
            <option value="">-- Tất cả các xã/phường ({wards.length}) --</option>
            {wards.map((w) => (
              <option key={w.code} value={w.code}>
                {w.fullName || w.name} ({w.code})
              </option>
            ))}
          </select>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-100 flex items-start space-x-3 text-rose-800 text-xs">
            <AlertCircle size={16} className="mt-0.5 shrink-0 text-rose-500" />
            <div className="flex-1">{error}</div>
          </div>
        )}

        {success && (
          <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-100 flex items-start space-x-3 text-emerald-800 text-xs">
            <Info size={16} className="mt-0.5 shrink-0 text-emerald-500" />
            <div className="flex-1">{success}</div>
          </div>
        )}

        {/* Table Content */}
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center space-y-3">
            <Loader2 className="animate-spin text-orange-600" size={32} />
            <span className="text-xs text-neutral-500">Đang tải dữ liệu OCOP...</span>
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="py-12 text-center text-neutral-400 text-xs border border-dashed border-neutral-200 rounded-xl">
            Không tìm thấy sản phẩm OCOP nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-neutral-100 text-[11px] font-semibold text-neutral-400 uppercase tracking-wider">
                  <th className="py-3 px-4">Sản phẩm</th>
                  <th className="py-3 px-4">Phân loại</th>
                  <th className="py-3 px-4">Địa bàn xã/phường</th>
                  <th className="py-3 px-4">Tọa độ</th>
                  {isAdmin && <th className="py-3 px-4 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 text-xs">
                {filteredProducts.map((item) => (
                  <tr key={item.id} className="hover:bg-neutral-50/70 transition-colors">
                    <td className="py-3.5 px-4">
                      <div className="flex items-center space-x-3">
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.name}
                            className="w-10 h-10 rounded-lg object-cover border border-neutral-200"
                          />
                        ) : (
                          <div className="w-10 h-10 rounded-lg bg-orange-50 flex items-center justify-center text-orange-600 text-xs font-bold border border-orange-100">
                            {item.name.charAt(0)}
                          </div>
                        )}
                        <div>
                          <span className="font-bold text-neutral-900 block">{item.name}</span>
                          {item.description && (
                            <span className="text-[11px] text-neutral-400 line-clamp-1 max-w-xs">
                              {item.description}
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-orange-50 text-orange-700 border border-orange-200">
                        {item.productType || 'Nông sản'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-neutral-600">
                      {item.wardName || item.wardCode}
                    </td>
                    <td className="py-3.5 px-4 text-neutral-500 font-mono text-[11px]">
                      <div className="flex items-center space-x-1">
                        <MapPin size={12} className="text-orange-500" />
                        <span>{item.latitude.toFixed(4)}, {item.longitude.toFixed(4)}</span>
                      </div>
                    </td>
                    {isAdmin && (
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => {
                              setEditingProduct(item);
                              setShowFormModal(true);
                            }}
                            className="p-1.5 text-neutral-500 hover:text-orange-600 hover:bg-orange-50 rounded-lg transition-colors cursor-pointer"
                            title="Sửa sản phẩm"
                          >
                            <Edit2 size={14} />
                          </button>
                          <button
                            onClick={() => setDeletingProduct(item)}
                            className="p-1.5 text-neutral-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa sản phẩm"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination controls */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-6 pt-4 border-t border-neutral-100 text-xs text-neutral-500">
            <span>Trang {currentPage + 1} / {totalPages}</span>
            <div className="flex items-center space-x-2">
              <button
                disabled={currentPage === 0 || loading}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="p-2 rounded-lg border border-neutral-200 disabled:opacity-40 hover:bg-neutral-50 cursor-pointer"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                disabled={currentPage >= totalPages - 1 || loading}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="p-2 rounded-lg border border-neutral-200 disabled:opacity-40 hover:bg-neutral-50 cursor-pointer"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}

      </div>

      {/* Add / Edit Modal - key prop ensures fresh mount on edit/create */}
      <OcopFormModal
        key={editingProduct?.id || 'new'}
        isOpen={showFormModal}
        onClose={() => setShowFormModal(false)}
        onSuccess={handleSuccess}
        onError={handleError}
        initialData={editingProduct}
      />

      {/* Delete Confirmation Modal */}
      {deletingProduct && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-sm w-full p-6 relative animate-zoomIn">
            <h3 className="text-base font-bold text-neutral-900 mb-2">Xác nhận xóa sản phẩm</h3>
            <p className="text-xs text-neutral-500 mb-6">
              Bạn có chắc chắn muốn xóa sản phẩm OCOP <span className="font-semibold text-neutral-800">"{deletingProduct.name}"</span>? Thao tác này không thể hoàn tác.
            </p>
            <div className="flex items-center justify-end space-x-3">
              <button
                onClick={() => setDeletingProduct(null)}
                disabled={deleteLoading}
                className="px-4 py-2 rounded-xl border border-neutral-200 text-xs font-semibold text-neutral-600 hover:bg-neutral-50 cursor-pointer"
              >
                Hủy
              </button>
              <button
                onClick={handleDeleteConfirm}
                disabled={deleteLoading}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-xs font-semibold text-white cursor-pointer flex items-center space-x-1.5"
              >
                {deleteLoading && <Loader2 size={14} className="animate-spin" />}
                <span>Xóa</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default React.memo(OcopPanel);
