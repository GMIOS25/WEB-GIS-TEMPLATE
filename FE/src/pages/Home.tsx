import React, { useState, useEffect, useCallback } from 'react';
import api from '../api/axiosInstance';
import { Map, X } from 'lucide-react';
import GisMap, { type GeoJsonData, type GeoJsonFeature } from './home/components/GisMap';
import SidebarDrawer from './home/components/SidebarDrawer';
import MapSearch from './home/components/MapSearch';
import ProfileCard from './home/components/ProfileCard';
import StatsBoard from './home/components/StatsBoard';
import DetailsPanel from './home/components/DetailsPanel';
import AdminPanel from './home/components/AdminPanel';

const Home: React.FC = () => {
  // Core view & drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [activeView, setActiveView] = useState<'map' | 'admin'>('map');

  // Map settings and GeoJSON data state
  const [layers, setLayers] = useState({
    province: true,
    commune: true,
  });
  const [geoJsonData, setGeoJsonData] = useState<GeoJsonData | null>(null);
  const [provinceGeoJson, setProvinceGeoJson] = useState<unknown>(null);
  const [mapLoading, setMapLoading] = useState<boolean>(true);
  const [selectedWard, setSelectedWard] = useState<GeoJsonFeature | null>(null);

  // 1. Fetch GIS Data on mount
  useEffect(() => {
    const fetchGisData = async () => {
      setMapLoading(true);
      try {
        const [wardsRes, provinceRes] = await Promise.all([
          api.get('/api/wards/geojson'),
          api.get('/api/wards/province/geojson'),
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

  const toggleLayer = useCallback((layer: 'province' | 'commune') => {
    setLayers((prev) => ({
      ...prev,
      [layer]: !prev[layer],
    }));
  }, []);

  return (
    <div className="w-full h-screen relative bg-white overflow-hidden font-sans text-neutral-900 select-none">
      
      {/* 1. VIEW PORT ROUTER */}
      <div className="absolute inset-0 z-0 bg-neutral-100 flex items-center justify-center">
        {mapLoading && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-sm z-30 flex flex-col items-center justify-center space-y-4 transition-all duration-300">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500"></div>
            <span className="text-sm font-semibold text-neutral-600">Đang tải bản đồ địa giới Gia Lai...</span>
          </div>
        )}

        {activeView === 'map' ? (
          <GisMap
            layers={layers}
            geoJsonData={geoJsonData}
            provinceGeoJson={provinceGeoJson}
            selectedWard={selectedWard}
            setSelectedWard={setSelectedWard}
          />
        ) : (
          <AdminPanel setActiveView={setActiveView} />
        )}
      </div>

      {/* 2. FLOATING OVERLAYS (Only visible on Map View) */}
      {activeView === 'map' && (
        <>
          {/* Top Left Search - remounts using key to sync input value */}
          <MapSearch
            key={selectedWard?.properties.code || 'empty'}
            geoJsonData={geoJsonData}
            selectedWard={selectedWard}
            setSelectedWard={setSelectedWard}
          />

          {/* Left Sidebar Drawer */}
          <SidebarDrawer
            isDrawerOpen={isDrawerOpen}
            setIsDrawerOpen={setIsDrawerOpen}
            layers={layers}
            toggleLayer={toggleLayer}
            activeView={activeView}
            setActiveView={setActiveView}
          />

          {/* Bottom Left Drawer Toggle FAB */}
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

          {/* Bottom Right Stats Board */}
          <StatsBoard geoJsonData={geoJsonData} />

          {/* Right Sidebar Details Panel */}
          <DetailsPanel
            selectedWard={selectedWard}
            setSelectedWard={setSelectedWard}
          />
        </>
      )}

      {/* Profile Card & Dropdown (Visible on both Map and Admin view for header navigation) */}
      <ProfileCard />

    </div>
  );
};

export default Home;
