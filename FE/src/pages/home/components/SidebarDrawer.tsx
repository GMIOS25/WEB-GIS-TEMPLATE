import React from 'react';
import { useAuth } from '../../../context/AuthContext';
import { ChevronDown, CheckSquare, Square, Users } from 'lucide-react';

interface SidebarDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (open: boolean) => void;
  layers: {
    province: boolean;
    commune: boolean;
  };
  toggleLayer: (layer: 'province' | 'commune') => void;
  activeView: 'map' | 'admin';
  setActiveView: (view: 'map' | 'admin') => void;
}

const SidebarDrawer: React.FC<SidebarDrawerProps> = ({
  isDrawerOpen,
  setIsDrawerOpen,
  layers,
  toggleLayer,
  activeView,
  setActiveView,
}) => {
  const { user } = useAuth();

  if (!isDrawerOpen) return null;

  return (
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
  );
};

export default React.memo(SidebarDrawer);
