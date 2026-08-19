import React from 'react';
import { useAuth } from '../../../context/AuthContext';
import { ChevronDown, CheckSquare, Square, Users, Sparkles, FlaskConical, Trees } from 'lucide-react';
import { FEATURE_FLAGS } from '../../../config/features';

export type ActiveViewType = 'map' | 'admin' | 'ocop' | 'science' | 'agriculture';

interface SidebarDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (open: boolean) => void;
  layers: {
    province: boolean;
    commune: boolean;
  };
  toggleLayer: (layer: 'province' | 'commune') => void;
  activeView: ActiveViewType;
  setActiveView: (view: ActiveViewType) => void;
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
  const hasFeatureModules = FEATURE_FLAGS.ocop || FEATURE_FLAGS.science || FEATURE_FLAGS.agriculture;

  if (!isDrawerOpen) return null;

  return (
    <div className="absolute top-6 bottom-[96px] left-6 w-[340px] bg-white border border-neutral-200 rounded-2xl shadow-lg p-6 flex flex-col justify-between z-20 animate-slideRight overflow-y-auto">
      <div className="space-y-6">
        <div>
          <h3 className="text-lg font-bold text-neutral-900">Bản đồ & Chuyên đề</h3>
          <p className="text-xs text-neutral-500 mt-1">Lớp dữ liệu và các module quản lý chuyên đề</p>
        </div>

        {/* Expandable Boundary Layer Group */}
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

        {/* Modular Feature Extensions Group */}
        {hasFeatureModules && (
          <div className="space-y-3 pt-2">
            <div className="flex items-center justify-between text-sm font-bold text-neutral-700 border-b border-neutral-100 pb-2">
              <span>Đơn vị trực thuộc & Chuyên đề</span>
            </div>

            <div className="space-y-2">
              {FEATURE_FLAGS.ocop && (
                <button
                  onClick={() => {
                    setActiveView(activeView === 'ocop' ? 'map' : 'ocop');
                    setIsDrawerOpen(false);
                  }}
                  className={`w-full py-2.5 px-3.5 rounded-xl border text-xs font-semibold flex items-center space-x-2.5 transition-all cursor-pointer ${
                    activeView === 'ocop'
                      ? 'bg-orange-500 text-white border-orange-500 shadow-sm shadow-orange-500/20'
                      : 'bg-orange-50/60 hover:bg-orange-100/80 text-orange-700 border-orange-200/80'
                  }`}
                >
                  <Sparkles size={16} className={activeView === 'ocop' ? 'text-white' : 'text-orange-600'} />
                  <span>Sản phẩm OCOP</span>
                </button>
              )}

              {FEATURE_FLAGS.science && (
                <button
                  onClick={() => {
                    setActiveView(activeView === 'science' ? 'map' : 'science');
                    setIsDrawerOpen(false);
                  }}
                  className={`w-full py-2.5 px-3.5 rounded-xl border text-xs font-semibold flex items-center space-x-2.5 transition-all cursor-pointer ${
                    activeView === 'science'
                      ? 'bg-slate-700 text-white border-slate-700 shadow-sm shadow-slate-700/20'
                      : 'bg-slate-100/80 hover:bg-slate-200/80 text-slate-800 border-slate-300/80'
                  }`}
                >
                  <FlaskConical size={16} className={activeView === 'science' ? 'text-white' : 'text-slate-700'} />
                  <span>Đơn vị Khoa học & CN</span>
                </button>
              )}

              {FEATURE_FLAGS.agriculture && (
                <button
                  onClick={() => {
                    setActiveView(activeView === 'agriculture' ? 'map' : 'agriculture');
                    setIsDrawerOpen(false);
                  }}
                  className={`w-full py-2.5 px-3.5 rounded-xl border text-xs font-semibold flex items-center space-x-2.5 transition-all cursor-pointer ${
                    activeView === 'agriculture'
                      ? 'bg-neutral-800 text-white border-neutral-800 shadow-sm shadow-neutral-800/20'
                      : 'bg-neutral-100 hover:bg-neutral-200/80 text-neutral-800 border-neutral-300/80'
                  }`}
                >
                  <Trees size={16} className={activeView === 'agriculture' ? 'text-white' : 'text-neutral-700'} />
                  <span>Đơn vị Nông nghiệp</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Drawer Admin Button: Only visible to ADMIN role */}
      <div className="pt-4 border-t border-neutral-100">
        {user?.role === 'ADMIN' && (
          <button
            onClick={() => {
              setActiveView(activeView === 'admin' ? 'map' : 'admin');
              setIsDrawerOpen(false);
            }}
            className="w-full py-3 px-4 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-semibold text-sm transition-all duration-200 flex items-center justify-center space-x-2 cursor-pointer shadow-sm shadow-primary-500/10"
          >
            <Users size={16} />
            <span>
              {activeView === 'admin' ? 'Quay lại bản đồ' : 'Quản trị người dùng'}
            </span>
          </button>
        )}
      </div>
    </div>
  );
};

export default React.memo(SidebarDrawer);
