import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { Trees, Plus, AlertCircle, Info, Edit2, Trash2, MapPin, ChevronLeft, ChevronRight, ArrowLeft, Loader2 } from 'lucide-react';
import { fetchAgricultureUnits, deleteAgricultureUnit } from '../../../api/agriculture';
import api from '../../../api/axiosInstance';
import AgricultureFormModal from './AgricultureFormModal';
import { extractErrorMessage } from '../../../api/errorUtils';
import type { AgricultureUnit } from '../../../types/agriculture';
import type { Ward } from '../../../types/gis';

interface AgriculturePanelProps {
  setActiveView: (view: 'map' | 'admin' | 'ocop' | 'science' | 'agriculture') => void;
}

const AgriculturePanel: React.FC<AgriculturePanelProps> = ({ setActiveView }) => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [units, setUnits] = useState<AgricultureUnit[]>([]);
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
  const [editingUnit, setEditingUnit] = useState<AgricultureUnit | null>(null);
  const [deletingUnit, setDeletingUnit] = useState<AgricultureUnit | null>(null);
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

  // Fetch Agriculture units on param change
  useEffect(() => {
    let active = true;
    fetchAgricultureUnits({
      page: currentPage,
      size: 10,
      wardCode: selectedWard || undefined,
    })
      .then((data) => {
        if (active) {
          setUnits(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          console.error(err);
          setError(extractErrorMessage(err, 'Không thể tải danh sách đơn vị nông nghiệp.'));
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [currentPage, selectedWard]);

  const loadUnits = useCallback(() => {
    setLoading(true);
    fetchAgricultureUnits({
      page: currentPage,
      size: 10,
      wardCode: selectedWard || undefined,
    })
      .then((data) => {
        setUnits(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        console.error(err);
        setError(extractErrorMessage(err, 'Không thể tải danh sách đơn vị nông nghiệp.'));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [currentPage, selectedWard]);

  const handleSuccess = (msg: string) => {
    setSuccess(msg);
    setError(null);
    loadUnits();
  };

  const handleError = (msg: string) => {
    setError(msg);
    setSuccess(null);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingUnit) return;
    setDeleteLoading(true);
    try {
      await deleteAgricultureUnit(deletingUnit.id);
      setSuccess(`Đã xóa đơn vị "${deletingUnit.name}" thành công.`);
      setDeletingUnit(null);
      loadUnits();
    } catch (err: unknown) {
      console.error(err);
      setError(extractErrorMessage(err, 'Có lỗi xảy ra khi xóa đơn vị nông nghiệp.'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Filter client-side search query
  const filteredUnits = units.filter((u) =>
    searchTerm ? u.name.toLowerCase().includes(searchTerm.toLowerCase()) || u.unitType?.toLowerCase().includes(searchTerm.toLowerCase()) : true
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
            <div className="w-10 h-10 rounded-xl bg-neutral-100 flex items-center justify-center text-neutral-700 border border-neutral-200">
              <Trees size={20} />
            </div>
            <div>
              <h3 className="text-xl font-bold text-neutral-900">Quản lý Đơn vị Nông nghiệp & Trang trại</h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                Cơ sở sản xuất, nông trường và hợp tác xã tỉnh Gia Lai (Tổng số: {totalElements})
              </p>
            </div>
          </div>

          {isAdmin && (
            <button
              onClick={() => {
                setEditingUnit(null);
                setShowFormModal(true);
              }}
              className="py-2.5 px-4 rounded-xl bg-neutral-800 hover:bg-neutral-900 active:scale-[0.98] text-white text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-sm shadow-neutral-800/20 cursor-pointer self-start sm:self-auto"
            >
              <Plus size={16} />
              <span>Thêm đơn vị nông nghiệp</span>
            </button>
          )}
        </div>

        {/* Filters and search */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
          <input
            type="text"
            placeholder="Tìm theo tên hoặc loại hình..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-neutral-500/20 focus:border-neutral-500"
          />
          <select
            value={selectedWard}
            onChange={(e) => {
              setSelectedWard(e.target.value);
              setCurrentPage(0);
            }}
            className="px-3.5 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-neutral-500/20 focus:border-neutral-500"
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
            <Loader2 className="animate-spin text-neutral-700" size={32} />
            <span className="text-xs text-neutral-500">Đang tải dữ liệu đơn vị nông nghiệp...</span>
          </div>
        ) : filteredUnits.length === 0 ? (
          <div className="py-12 text-center text-neutral-400 text-xs border border-dashed border-neutral-200 rounded-xl">
            Không tìm thấy đơn vị nông nghiệp nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-neutral-100 text-[11px] font-semibold text-neutral-400 uppercase tracking-wider">
                  <th className="py-3 px-4">Đơn vị</th>
                  <th className="py-3 px-4">Loại hình</th>
                  <th className="py-3 px-4">Địa bàn xã/phường</th>
                  <th className="py-3 px-4">Tọa độ</th>
                  {isAdmin && <th className="py-3 px-4 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 text-xs">
                {filteredUnits.map((item) => (
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
                          <div className="w-10 h-10 rounded-lg bg-neutral-100 flex items-center justify-center text-neutral-700 text-xs font-bold border border-neutral-200">
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
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-neutral-100 text-neutral-800 border border-neutral-200">
                        {item.unitType || 'Nông nghiệp'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-neutral-600">
                      {item.wardName || item.wardCode}
                    </td>
                    <td className="py-3.5 px-4 text-neutral-500 font-mono text-[11px]">
                      <div className="flex items-center space-x-1">
                        <MapPin size={12} className="text-neutral-600" />
                        <span>{item.latitude.toFixed(4)}, {item.longitude.toFixed(4)}</span>
                      </div>
                    </td>
                    {isAdmin && (
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => {
                              setEditingUnit(item);
                              setShowFormModal(true);
                            }}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Sửa đơn vị"
                          >
                            <Edit2 size={14} />
                          </button>
                          <button
                            onClick={() => setDeletingUnit(item)}
                            className="p-1.5 text-neutral-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa đơn vị"
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

      {/* Add / Edit Modal - key prop ensures fresh mount */}
      <AgricultureFormModal
        key={editingUnit?.id || 'new'}
        isOpen={showFormModal}
        onClose={() => setShowFormModal(false)}
        onSuccess={handleSuccess}
        onError={handleError}
        initialData={editingUnit}
      />

      {/* Delete Confirmation Modal */}
      {deletingUnit && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-sm w-full p-6 relative animate-zoomIn">
            <h3 className="text-base font-bold text-neutral-900 mb-2">Xác nhận xóa đơn vị</h3>
            <p className="text-xs text-neutral-500 mb-6">
              Bạn có chắc chắn muốn xóa đơn vị nông nghiệp <span className="font-semibold text-neutral-800">"{deletingUnit.name}"</span>? Thao tác này không thể hoàn tác.
            </p>
            <div className="flex items-center justify-end space-x-3">
              <button
                onClick={() => setDeletingUnit(null)}
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

export default React.memo(AgriculturePanel);
