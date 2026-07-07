import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosInstance';
import { Map, X, ChevronDown, User as UserIcon, LogOut, Shield, CheckSquare, Square, Users, Search, Plus, Key, Trash2, Edit2, AlertCircle, Info, Landmark } from 'lucide-react';
import { MapContainer, TileLayer, GeoJSON, useMap } from 'react-leaflet';
import L from 'leaflet';

// Fix Leaflet marker icon issues in Vite/Webpack if markers are used
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
});

// Map controller sub-component to handle programmatically flying to selected ward boundaries
const MapController: React.FC<{ selectedWard: any }> = ({ selectedWard }) => {
  const map = useMap();
  useEffect(() => {
    if (selectedWard && selectedWard.geometry) {
      const geojsonFeature = selectedWard;
      const layer = L.geoJSON(geojsonFeature);
      const bounds = layer.getBounds();
      if (bounds.isValid()) {
        map.flyToBounds(bounds, { maxZoom: 13, duration: 1.2 });
      }
    }
  }, [selectedWard, map]);
  return null;
};

const Home: React.FC = () => {
  const { user, logout } = useAuth();

  // State controls
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const [activeView, setActiveView] = useState<'map' | 'admin'>('map');

  // Map settings and geo data
  const [layers, setLayers] = useState({
    province: true,
    commune: true,
  });
  const [geoJsonData, setGeoJsonData] = useState<any>(null);
  const [provinceGeoJson, setProvinceGeoJson] = useState<any>(null);
  const [mapLoading, setMapLoading] = useState<boolean>(true);
  const [selectedWard, setSelectedWard] = useState<any>(null);

  // Search Autocomplete state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const autocompleteRef = useRef<HTMLDivElement>(null);

  // User management (Admin CRUD) state
  const [users, setUsers] = useState<any[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [adminError, setAdminError] = useState<string | null>(null);
  const [adminSuccess, setAdminSuccess] = useState<string | null>(null);

  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [targetUser, setTargetUser] = useState<any>(null);

  // Modal form states
  const [addUsername, setAddUsername] = useState('');
  const [addFullName, setAddFullName] = useState('');
  const [addPassword, setAddPassword] = useState('');
  const [editFullName, setEditFullName] = useState('');
  const [editPassword, setEditPassword] = useState('');
  const [editConfirmPassword, setEditConfirmPassword] = useState('');

  // 1. Fetch GIS Data on mount
  useEffect(() => {
    const fetchGisData = async () => {
      setMapLoading(true);
      try {
        const [wardsRes, provinceRes] = await Promise.all([
          api.get('/api/wards/geojson'),
          api.get('/api/wards/province/geojson')
        ]);
        setGeoJsonData(wardsRes.data);
        setProvinceGeoJson(provinceRes.data);
      } catch (err) {
        console.error('Failed to load GIS boundary data', err);
      } finally {
        setMapLoading(false);
      }
    };
    fetchGisData();
  }, []);

  // 2. Fetch Users list for Admin panel
  const fetchUsers = async () => {
    if (user?.role !== 'ADMIN') return;
    setUsersLoading(true);
    setAdminError(null);
    try {
      const response = await api.get('/api/admin/users');
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to load users list', err);
      setAdminError('Không thể tải danh sách tài khoản từ máy chủ.');
    } finally {
      setUsersLoading(false);
    }
  };

  useEffect(() => {
    if (activeView === 'admin') {
      fetchUsers();
    }
  }, [activeView]);

  // Handle local Autocomplete filtering
  useEffect(() => {
    if (!searchQuery.trim() || !geoJsonData) {
      setSearchResults([]);
      return;
    }
    const filtered = geoJsonData.features.filter((f: any) =>
      f.properties.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.properties.name?.toLowerCase().includes(searchQuery.toLowerCase())
    );
    setSearchResults(filtered.slice(0, 5)); // Limit to top 5 results
  }, [searchQuery, geoJsonData]);

  // Close search suggestions on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (autocompleteRef.current && !autocompleteRef.current.contains(e.target as Node)) {
        setIsSearchFocused(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggleLayer = (layer: 'province' | 'commune') => {
    setLayers((prev) => ({
      ...prev,
      [layer]: !prev[layer],
    }));
  };

  const handleLogout = () => {
    logout();
  };

  // Add User submit handler
  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setAdminError(null);
    setAdminSuccess(null);
    if (!addUsername.trim() || !addFullName.trim() || !addPassword.trim()) {
      setAdminError('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }
    try {
      await api.post('/api/admin/users', {
        username: addUsername.trim(),
        fullName: addFullName.trim(),
        password: addPassword.trim(),
      });
      setAdminSuccess(`Đã tạo tài khoản "${addUsername}" thành công.`);
      setShowAddModal(false);
      // Reset form state
      setAddUsername('');
      setAddFullName('');
      setAddPassword('');
      // Refresh list
      fetchUsers();
    } catch (err: any) {
      console.error(err);
      setAdminError(err.response?.data || 'Có lỗi xảy ra khi tạo tài khoản.');
    }
  };

  // Edit User submit handler
  const handleEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setAdminError(null);
    setAdminSuccess(null);
    if (!editFullName.trim()) {
      setAdminError('Họ và tên không được để trống.');
      return;
    }
    if (editPassword && editPassword !== editConfirmPassword) {
      setAdminError('Mật khẩu nhập lại không khớp.');
      return;
    }
    try {
      await api.put(`/api/admin/users/${targetUser.id}`, {
        fullName: editFullName.trim(),
        password: editPassword || undefined,
      });
      setAdminSuccess(`Cập nhật tài khoản "${targetUser.username}" thành công.`);
      setShowEditModal(false);
      // Reset fields
      setEditFullName('');
      setEditPassword('');
      setEditConfirmPassword('');
      setTargetUser(null);
      // Refresh list
      fetchUsers();
    } catch (err: any) {
      console.error(err);
      setAdminError(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật.');
    }
  };

  // Delete User handler
  const handleDeleteUser = async () => {
    setAdminError(null);
    setAdminSuccess(null);
    if (!targetUser) return;
    try {
      await api.delete(`/api/admin/users/${targetUser.id}`);
      setAdminSuccess(`Đã xóa tài khoản "${targetUser.username}" thành công.`);
      setShowDeleteModal(false);
      setTargetUser(null);
      fetchUsers();
    } catch (err: any) {
      console.error(err);
      setAdminError(err.response?.data || 'Không thể xóa tài khoản này.');
    }
  };

  // Calculate dynamic stats
  const totalWards = geoJsonData ? geoJsonData.features.length : 0;
  const totalArea = geoJsonData 
    ? geoJsonData.features.reduce((sum: number, f: any) => sum + (Number(f.properties.areaKm2) || 0), 0)
    : 0;

  // Leaflet Layer Interactive Styles
  const onEachFeature = (feature: any, layer: any) => {
    layer.on({
      mouseover: (e: any) => {
        const hoverLayer = e.target;
        hoverLayer.setStyle({
          weight: 3,
          color: '#059669',
          fillColor: '#6ee7b7',
          fillOpacity: 0.35,
        });
        if (!L.Browser.ie && !L.Browser.opera && !L.Browser.edge) {
          hoverLayer.bringToFront();
        }
      },
      mouseout: (e: any) => {
        const hoverLayer = e.target;
        const isSelected = selectedWard && selectedWard.properties.code === feature.properties.code;
        hoverLayer.setStyle({
          weight: isSelected ? 3 : 1,
          color: isSelected ? '#10b981' : '#6b7280',
          fillColor: isSelected ? '#a7f3d0' : '#10b981',
          fillOpacity: isSelected ? 0.3 : 0.08,
        });
      },
      click: () => {
        setSelectedWard(feature);
      },
    });
  };

  return (
    <div className="w-full h-screen relative bg-white overflow-hidden font-sans text-neutral-900 select-none">
      
      {/* 1. MAP VIEW CONTAINER */}
      <div className="absolute inset-0 z-0 bg-neutral-100 flex items-center justify-center">
        {mapLoading && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-sm z-30 flex flex-col items-center justify-center space-y-4 transition-all duration-300">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500"></div>
            <span className="text-sm font-semibold text-neutral-600">Đang tải bản đồ địa giới Gia Lai...</span>
          </div>
        )}

        {activeView === 'map' ? (
          <MapContainer 
            center={[13.8, 108.2]} 
            zoom={9} 
            scrollWheelZoom={true} 
            zoomControl={false}
            className="w-full h-full"
          >
            <TileLayer
  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
  url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
/>
            
            {/* Wards Boundary layer */}
            {layers.commune && geoJsonData && (
              <GeoJSON 
                data={geoJsonData} 
                style={(feature: any) => {
                  const isSelected = selectedWard && selectedWard.properties.code === feature?.properties.code;
                  return {
                    fillColor: isSelected ? '#a7f3d0' : '#10b981',
                    weight: isSelected ? 3 : 1,
                    opacity: 1,
                    color: isSelected ? '#059669' : '#6b7280',
                    fillOpacity: isSelected ? 0.3 : 0.08,
                  };
                }}
                onEachFeature={onEachFeature}
              />
            )}

            {/* Province Outline boundary */}
            {layers.province && provinceGeoJson && (
              <GeoJSON 
                data={provinceGeoJson} 
                style={{
                  fillColor: 'transparent',
                  weight: 3,
                  color: '#047857',
                  opacity: 1,
                  fillOpacity: 0
                }}
                interactive={false}
              />
            )}

            <MapController selectedWard={selectedWard} />
          </MapContainer>
        ) : (
          /* 2. ADMIN VIEW OVERLAY */
          <div className="w-full h-full bg-neutral-50 overflow-y-auto z-20 flex flex-col p-6 sm:p-10 pt-24">
            <div className="w-full max-w-5xl mx-auto bg-white border border-neutral-200 rounded-2xl shadow-sm p-6 sm:p-8">
              
              {/* Header */}
              <div className="flex justify-between items-center mb-8 pb-4 border-b border-neutral-100">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500">
                    <Users size={20} />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-neutral-900">Quản lý người dùng</h3>
                    <p className="text-xs text-neutral-400 mt-0.5">Danh sách các tài khoản Viewer được phân quyền trong hệ thống</p>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <button 
                    onClick={() => {
                      setAdminError(null);
                      setAdminSuccess(null);
                      setShowAddModal(true);
                    }}
                    className="py-2.5 px-4 rounded-xl bg-primary-500 hover:bg-primary-600 active:scale-[0.98] text-white text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-sm shadow-primary-500/10 cursor-pointer"
                  >
                    <Plus size={16} />
                    <span>Thêm tài khoản</span>
                  </button>
                  <button 
                    onClick={() => setActiveView('map')}
                    className="py-2.5 px-4 rounded-xl bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-semibold cursor-pointer transition-all"
                  >
                    Quay lại bản đồ
                  </button>
                </div>
              </div>

              {/* Status Message Banners */}
              {adminError && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs sm:text-sm font-semibold flex items-center space-x-2">
                  <AlertCircle size={18} className="shrink-0" />
                  <span>{adminError}</span>
                </div>
              )}
              {adminSuccess && (
                <div className="mb-6 p-4 rounded-xl bg-green-50 border border-green-200 text-green-700 text-xs sm:text-sm font-semibold flex items-center space-x-2">
                  <Info size={18} className="shrink-0" />
                  <span>{adminSuccess}</span>
                </div>
              )}

              {/* Users Table */}
              <div className="overflow-x-auto border border-neutral-100 rounded-xl">
                <table className="w-full text-sm text-left text-neutral-500">
                  <thead className="text-xs text-neutral-700 uppercase bg-neutral-50 border-b border-neutral-100">
                    <tr>
                      <th className="px-6 py-4">Tên đăng nhập</th>
                      <th className="px-6 py-4">Họ và tên</th>
                      <th className="px-6 py-4">Vai trò</th>
                      <th className="px-6 py-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {usersLoading ? (
                      <tr>
                        <td colSpan={4} className="px-6 py-10 text-center text-neutral-400 font-semibold">
                          <div className="flex justify-center items-center space-x-2">
                            <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-primary-500"></div>
                            <span>Đang tải danh sách...</span>
                          </div>
                        </td>
                      </tr>
                    ) : users.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-6 py-10 text-center text-neutral-400 font-semibold">
                          Chưa có tài khoản Viewer nào.
                        </td>
                      </tr>
                    ) : (
                      users.map((item) => (
                        <tr key={item.id} className="bg-white hover:bg-neutral-50/50 transition-colors">
                          <td className="px-6 py-4 font-semibold text-neutral-900">@{item.username}</td>
                          <td className="px-6 py-4">{item.fullName}</td>
                          <td className="px-6 py-4">
                            <span className={`inline-flex items-center space-x-1 text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                              item.role === 'ADMIN' ? 'bg-red-50 text-red-700 border-red-200' : 'bg-primary-50 text-primary-700 border-primary-100'
                            }`}>
                              <Shield size={10} className="shrink-0" />
                              <span>{item.role}</span>
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right flex justify-end space-x-2">
                            <button
                              onClick={() => {
                                setTargetUser(item);
                                setEditFullName(item.fullName);
                                setEditPassword('');
                                setEditConfirmPassword('');
                                setAdminError(null);
                                setAdminSuccess(null);
                                setShowEditModal(true);
                              }}
                              className="p-2 text-neutral-400 hover:text-neutral-900 rounded-lg hover:bg-neutral-100 cursor-pointer transition-all"
                              title="Chỉnh sửa thông tin"
                            >
                              <Edit2 size={16} />
                            </button>
                            {item.username !== user?.username && (
                              <button
                                onClick={() => {
                                  setTargetUser(item);
                                  setAdminError(null);
                                  setAdminSuccess(null);
                                  setShowDeleteModal(true);
                                }}
                                className="p-2 text-neutral-400 hover:text-red-600 rounded-lg hover:bg-red-50 cursor-pointer transition-all"
                                title="Xóa tài khoản"
                              >
                                <Trash2 size={16} />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 3. TOP LEFT FLOATING SEARCH BAR (Autocomplete Map) */}
      {activeView === 'map' && (
        <div ref={autocompleteRef} className="absolute top-6 left-6 z-30 w-[340px] max-w-[calc(100vw-48px)]">
          <div className="relative bg-white border border-neutral-200 rounded-2xl shadow-md p-1.5 flex items-center space-x-2">
            <span className="pl-3 text-neutral-400">
              <Search size={18} />
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => setIsSearchFocused(true)}
              placeholder="Tìm kiếm xã/phường..."
              className="w-full py-2 pr-4 bg-transparent text-sm text-neutral-900 placeholder-neutral-400 focus:outline-none"
            />
            {searchQuery && (
              <button 
                onClick={() => {
                  setSearchQuery('');
                  setSelectedWard(null);
                }} 
                className="p-1 hover:bg-neutral-100 rounded-full text-neutral-400 hover:text-neutral-700 transition-all cursor-pointer"
              >
                <X size={16} />
              </button>
            )}
          </div>

          {/* Autocomplete Dropdown */}
          {isSearchFocused && searchResults.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-neutral-200 rounded-2xl shadow-lg p-2 overflow-hidden animate-fadeIn">
              {searchResults.map((f: any) => (
                <button
                  key={f.properties.code}
                  onClick={() => {
                    setSelectedWard(f);
                    setSearchQuery(f.properties.fullName);
                    setIsSearchFocused(false);
                  }}
                  className="w-full text-left px-4 py-3 hover:bg-neutral-50 rounded-xl flex items-center space-x-2 text-sm text-neutral-700 transition-colors cursor-pointer"
                >
                  <Landmark size={14} className="text-primary-500 shrink-0" />
                  <span className="font-semibold text-neutral-900">{f.properties.fullName}</span>
                </button>
              ))}
            </div>
          )}

          {isSearchFocused && searchQuery.trim() !== '' && searchResults.length === 0 && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-neutral-200 rounded-2xl shadow-lg p-4 text-center text-sm text-neutral-400 font-semibold animate-fadeIn">
              Không tìm thấy kết quả phù hợp.
            </div>
          )}
        </div>
      )}

      {/* 4. TOP RIGHT PROFILE CARD */}
      <div className="absolute top-6 right-6 z-30">
        <button
          onClick={() => setShowProfileDropdown(!showProfileDropdown)}
          className="bg-white border border-neutral-200 hover:border-neutral-300 rounded-xl px-5 py-3 shadow-sm hover:shadow-md flex items-center justify-between space-x-3 w-[240px] cursor-pointer transition-all duration-200"
        >
          <span className="text-sm font-bold text-neutral-900 truncate">
            {user?.fullName || 'Tên người dùng'}
          </span>
          <div className="w-8 h-8 rounded-full bg-neutral-50 flex items-center justify-center border border-neutral-200 text-neutral-600 shrink-0 shadow-inner">
            <UserIcon size={16} />
          </div>
        </button>

        {/* Profile Dropdown Menu */}
        {showProfileDropdown && (
          <div className="absolute right-0 mt-2 w-[240px] bg-white border border-neutral-200 rounded-xl shadow-lg p-3 z-50 animate-fadeIn">
            <div className="px-3 py-2.5 border-b border-neutral-100 mb-2">
              <p className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Tài khoản</p>
              <p className="text-sm font-semibold text-neutral-900 truncate mt-0.5">@{user?.username}</p>
              <div className="flex items-center space-x-1.5 mt-1.5">
                <Shield size={12} className="text-primary-500" />
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider">{user?.role}</span>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="w-full text-left px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 flex items-center space-x-2 transition-all cursor-pointer font-medium"
            >
              <LogOut size={16} />
              <span>Đăng xuất</span>
            </button>
          </div>
        )}
      </div>

      {/* 5. BOTTOM RIGHT STATISTICS BOARD (Only Map view) */}
      {activeView === 'map' && (
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
      )}

      {/* 6. RIGHT SIDEBAR DETAILS PANEL */}
      {activeView === 'map' && selectedWard && (
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
      )}

      {/* 7. LEFT SIDEBAR DRAWER */}
      {isDrawerOpen && (
        <div className="absolute top-6 bottom-[96px] left-6 w-[340px] bg-white border border-neutral-200 rounded-2xl shadow-lg p-6 flex flex-col justify-between z-20 animate-slideRight">
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-bold text-neutral-900">Bản đồ địa giới</h3>
              <p className="text-xs text-neutral-500 mt-1">Lớp dữ liệu bản đồ hành chính tỉnh Gia Lai</p>
            </div>

            {/* Expandable Layer Group */}
            <div className="space-y-3">
              <div className="flex items-center justify-between text-sm font-bold text-neutral-700 border-b border-neutral-100 pb-2">
                <span>Ranh giới hành chính</span>
                <ChevronDown size={16} className="text-neutral-500" />
              </div>

              {/* Layer Checkboxes */}
              <div className="space-y-2.5">
                <button
                  onClick={() => toggleLayer('province')}
                  className="w-full flex items-center space-x-3 text-sm text-neutral-600 hover:text-neutral-900 transition-colors py-1 cursor-pointer"
                >
                  {layers.province ? (
                    <CheckSquare size={18} className="text-primary-500 fill-primary-500/10" />
                  ) : (
                    <Square size={18} className="text-neutral-400" />
                  )}
                  <span className={layers.province ? 'font-semibold text-neutral-900' : ''}>Ranh giới cấp Tỉnh</span>
                </button>

                <button
                  onClick={() => toggleLayer('commune')}
                  className="w-full flex items-center space-x-3 text-sm text-neutral-600 hover:text-neutral-900 transition-colors py-1 cursor-pointer"
                >
                  {layers.commune ? (
                    <CheckSquare size={18} className="text-primary-500 fill-primary-500/10" />
                  ) : (
                    <Square size={18} className="text-neutral-400" />
                  )}
                  <span className={layers.commune ? 'font-semibold text-neutral-900' : ''}>Ranh giới cấp Xã</span>
                </button>
              </div>
            </div>
          </div>

          {/* Drawer Admin Button: Only visible to ADMIN role */}
          {user?.role === 'ADMIN' && (
            <button
              onClick={() => {
                setActiveView(activeView === 'map' ? 'admin' : 'map');
                setIsDrawerOpen(false); // Auto close drawer
              }}
              className="w-full py-3 px-4 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-semibold text-sm transition-all duration-200 flex items-center justify-center space-x-2 cursor-pointer shadow-sm shadow-primary-500/10"
            >
              <Users size={16} />
              <span>
                {activeView === 'map' ? 'Quản trị người dùng' : 'Quay lại bản đồ'}
              </span>
            </button>
          )}
        </div>
      )}

      {/* 8. DRAWER TOGGLE FAB */}
      <button
        onClick={() => setIsDrawerOpen(!isDrawerOpen)}
        className="absolute bottom-6 left-6 w-14 h-14 sm:w-[64px] sm:h-[64px] rounded-2xl bg-primary-500 hover:bg-primary-600 active:scale-[0.96] text-white flex items-center justify-center shadow-lg transition-all duration-200 z-50 cursor-pointer"
        aria-label="Toggle Navigation Drawer"
      >
        {isDrawerOpen ? (
          <X size={32} className="stroke-[2.5]" />
        ) : (
          <Map size={32} className="stroke-[2] animate-pulse" />
        )}
      </button>

      {/* ========================================================================= */}
      {/* CUSTOM DIALOG MODALS - DESIGN SYSTEM COMPLIANT & NO OUTSIDE RADIX/SHADCN */}
      {/* ========================================================================= */}

      {/* 1. ADD USER MODAL */}
      {showAddModal && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-md w-full p-6 relative animate-zoomIn">
            <button 
              onClick={() => setShowAddModal(false)}
              className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
            >
              <X size={18} />
            </button>
            <div className="flex items-center space-x-3 mb-6">
              <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500">
                <Plus size={20} />
              </div>
              <h3 className="text-lg font-bold text-neutral-900">Tạo tài khoản mới</h3>
            </div>
            
            <form onSubmit={handleAddUser} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Tên đăng nhập *</label>
                <input
                  type="text"
                  required
                  value={addUsername}
                  onChange={(e) => setAddUsername(e.target.value)}
                  placeholder="Nhập tên đăng nhập (ví dụ: viewer_le)"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Họ và tên *</label>
                <input
                  type="text"
                  required
                  value={addFullName}
                  onChange={(e) => setAddFullName(e.target.value)}
                  placeholder="Nhập họ và tên hiển thị"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Mật khẩu ban đầu *</label>
                <input
                  type="password"
                  required
                  value={addPassword}
                  onChange={(e) => setAddPassword(e.target.value)}
                  placeholder="Mật khẩu ít nhất 6 ký tự"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2.5 rounded-xl text-xs font-bold text-neutral-600 bg-neutral-100 hover:bg-neutral-200 cursor-pointer transition-all"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-primary-500 hover:bg-primary-600 active:scale-[0.98] shadow-sm shadow-primary-500/10 cursor-pointer transition-all"
                >
                  Tạo tài khoản
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. EDIT USER / PASSWORD MODAL */}
      {showEditModal && targetUser && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-md w-full p-6 relative animate-zoomIn">
            <button 
              onClick={() => {
                setShowEditModal(false);
                setTargetUser(null);
              }}
              className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
            >
              <X size={18} />
            </button>
            <div className="flex items-center space-x-3 mb-6">
              <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500">
                <Key size={20} />
              </div>
              <div>
                <h3 className="text-lg font-bold text-neutral-900">Cập nhật tài khoản</h3>
                <p className="text-xs text-neutral-400 mt-0.5">@{targetUser.username}</p>
              </div>
            </div>
            
            <form onSubmit={handleEditUser} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Họ và tên *</label>
                <input
                  type="text"
                  required
                  value={editFullName}
                  onChange={(e) => setEditFullName(e.target.value)}
                  placeholder="Họ và tên"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="space-y-1.5 pt-2">
                <p className="text-[10px] font-bold text-neutral-400 border-b border-neutral-100 pb-1 mb-2">Đổi mật khẩu (bỏ trống nếu giữ nguyên)</p>
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Mật khẩu mới</label>
                <input
                  type="password"
                  value={editPassword}
                  onChange={(e) => setEditPassword(e.target.value)}
                  placeholder="Mật khẩu mới ít nhất 6 ký tự"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Nhập lại mật khẩu mới</label>
                <input
                  type="password"
                  value={editConfirmPassword}
                  onChange={(e) => setEditConfirmPassword(e.target.value)}
                  placeholder="Xác nhận mật khẩu mới"
                  className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-neutral-100">
                <button
                  type="button"
                  onClick={() => {
                    setShowEditModal(false);
                    setTargetUser(null);
                  }}
                  className="px-4 py-2.5 rounded-xl text-xs font-bold text-neutral-600 bg-neutral-100 hover:bg-neutral-200 cursor-pointer transition-all"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-primary-500 hover:bg-primary-600 active:scale-[0.98] shadow-sm shadow-primary-500/10 cursor-pointer transition-all"
                >
                  Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. DELETE USER CONFIRMATION MODAL */}
      {showDeleteModal && targetUser && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-sm w-full p-6 relative animate-zoomIn">
            <button 
              onClick={() => {
                setShowDeleteModal(false);
                setTargetUser(null);
              }}
              className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
            >
              <X size={18} />
            </button>
            <div className="flex flex-col items-center text-center space-y-4 mb-6">
              <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center text-red-600">
                <Trash2 size={24} />
              </div>
              <div>
                <h3 className="text-lg font-bold text-neutral-900">Xác nhận xóa tài khoản</h3>
                <p className="text-xs text-neutral-500 mt-2">
                  Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản <strong className="text-neutral-950">@{targetUser.username}</strong> ({targetUser.fullName})? Hành động này không thể hoàn tác.
                </p>
              </div>
            </div>

            <div className="flex justify-stretch space-x-3">
              <button
                onClick={() => {
                  setShowDeleteModal(false);
                  setTargetUser(null);
                }}
                className="flex-1 py-2.5 rounded-xl text-xs font-bold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 cursor-pointer transition-all"
              >
                Hủy bỏ
              </button>
              <button
                onClick={handleDeleteUser}
                className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 active:scale-[0.98] cursor-pointer transition-all"
              >
                Đồng ý xóa
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default Home;
