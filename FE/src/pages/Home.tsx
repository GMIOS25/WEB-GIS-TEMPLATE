import React, { useState, useCallback, useTransition, Suspense, lazy } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Map, X } from 'lucide-react';
import GisMap, { type GeoJsonData, type GeoJsonFeature } from './home/components/GisMap';
import SidebarDrawer, { type ActiveViewType, type LayerKey, type MapLayersState } from './home/components/SidebarDrawer';
import MapSearch from './home/components/MapSearch';
import ProfileCard from './home/components/ProfileCard';
import StatsBoard from './home/components/StatsBoard';
import DetailsPanel, { type SelectedPoiDetail } from './home/components/DetailsPanel';
import RadiusSearchControl, { type RadiusSearchState } from './home/components/RadiusSearchControl';

// These 4 panels (+ every modal each of them imports: AddUserModal, EditUserModal,
// DeleteUserModal, OcopFormModal, ScienceFormModal, AgricultureFormModal) are only
// ever needed after the user actively navigates to them from the sidebar. Loading
// them lazily keeps them out of the initial JS bundle the browser has to download,
// parse and execute before the map becomes interactive — on a slower CPU that
// parse/compile step is a real, measurable chunk of the "lag" on first load.
const AdminPanel = lazy(() => import('./home/components/AdminPanel'));
const OcopPanel = lazy(() => import('./home/components/OcopPanel'));
const SciencePanel = lazy(() => import('./home/components/SciencePanel'));
const AgriculturePanel = lazy(() => import('./home/components/AgriculturePanel'));

const PanelLoadingFallback: React.FC = () => (
  <div className="w-full h-full flex items-center justify-center">
    <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-primary-500" />
  </div>
);

import api from '../api/axiosInstance';
import { queryKeys } from '../api/queryKeys';
import { FEATURE_FLAGS } from '../config/features';
import { fetchOcopGeoJson, fetchOcopProductById } from '../api/ocop';
import { fetchScienceGeoJson, fetchScienceUnitById } from '../api/science';
import { fetchAgricultureGeoJson, fetchAgricultureUnitById } from '../api/agriculture';

const Home: React.FC = () => {
  // Core view & drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [activeView, setActiveView] = useState<ActiveViewType>('map');
  const [, startTransition] = useTransition();

  // Map settings - Single Source of Truth for Layers
  const [layers, setLayers] = useState<MapLayersState>({
    province: false,
    commune: true,
    ocop: true,
    science: true,
    agriculture: true,
  });

  const [selectedWard, setSelectedWard] = useState<GeoJsonFeature | null>(null);
  const [selectedPoi, setSelectedPoi] = useState<SelectedPoiDetail | null>(null);

  // Radius Search State
  const [radiusSearchState, setRadiusSearchState] = useState<RadiusSearchState>({
    center: null,
    radiusKm: 10,
    module: 'ocop',
    resultIds: [],
  });
  const [isPickingCenter, setIsPickingCenter] = useState(false);

  // 1. Fetch GIS Boundary Data using TanStack Query
  const { data: geoJsonData = null, isLoading: isWardsLoading } = useQuery<GeoJsonData>({
    queryKey: queryKeys.wards.geojson(),
    queryFn: async () => {
      const res = await api.get<GeoJsonData>('/api/wards/geojson');
      return res.data;
    },
    staleTime: 1000 * 60 * 30, // 30 mins
  });

  const { data: provinceGeoJson = null } = useQuery({
    queryKey: queryKeys.wards.provinceGeojson(),
    queryFn: async () => {
      const res = await api.get('/api/wards/province/geojson');
      return res.data;
    },
    staleTime: 1000 * 60 * 60, // 1 hour
  });

  // 2. Fetch POI GeoJSON Data conditionally using TanStack Query
  const { data: ocopGeoJson = null } = useQuery({
    queryKey: queryKeys.ocop.geojson(),
    queryFn: fetchOcopGeoJson,
    enabled: FEATURE_FLAGS.ocop && layers.ocop,
    staleTime: 1000 * 60 * 10,
  });

  const { data: scienceGeoJson = null } = useQuery({
    queryKey: queryKeys.science.geojson(),
    queryFn: fetchScienceGeoJson,
    enabled: FEATURE_FLAGS.science && layers.science,
    staleTime: 1000 * 60 * 10,
  });

  const { data: agricultureGeoJson = null } = useQuery({
    queryKey: queryKeys.agriculture.geojson(),
    queryFn: fetchAgricultureGeoJson,
    enabled: FEATURE_FLAGS.agriculture && layers.agriculture,
    staleTime: 1000 * 60 * 10,
  });

  // Toggle Layer handler
  const toggleLayer = useCallback((layer: LayerKey) => {
    setLayers((prev) => ({
      ...prev,
      [layer]: !prev[layer],
    }));
  }, []);

  // Map center picked for radius search
  const handleMapCenterPicked = useCallback((lat: number, lng: number) => {
    setRadiusSearchState((prev) => ({
      ...prev,
      center: [lat, lng],
    }));
    setIsPickingCenter(false);
  }, []);

  // Lazy POI detail fetcher when user clicks [Xem chi tiết] on marker popup
  const handleSelectPoiDetail = useCallback(async (type: 'ocop' | 'science' | 'agriculture', id: number) => {
    try {
      if (type === 'ocop') {
        const item = await fetchOcopProductById(id);
        startTransition(() => {
          setSelectedPoi({
            moduleType: 'ocop',
            id: item.id,
            name: item.name,
            typeBadge: item.productTypes && item.productTypes.length > 0 ? item.productTypes[0] : 'Nông sản',
            productTypes: item.productTypes,
            starRating: item.starRating,
            contactPhone: item.contactPhone,
            locationAddress: item.locationAddress,
            wardCode: item.wardCode,
            wardName: item.wardName,
            latitude: item.latitude,
            longitude: item.longitude,
            imageUrl: item.imageUrl,
          });
          setSelectedWard(null);
        });
      } else if (type === 'science') {
        const item = await fetchScienceUnitById(id);
        startTransition(() => {
          setSelectedPoi({
            moduleType: 'science',
            id: item.id,
            name: item.name,
            typeBadge: item.unitType,
            description: item.description,
            wardCode: item.wardCode,
            wardName: item.wardName,
            latitude: item.latitude,
            longitude: item.longitude,
            imageUrl: item.imageUrl,
          });
          setSelectedWard(null);
        });
      } else if (type === 'agriculture') {
        const item = await fetchAgricultureUnitById(id);
        startTransition(() => {
          setSelectedPoi({
            moduleType: 'agriculture',
            id: item.id,
            name: item.name,
            typeBadge: item.unitType,
            description: item.description,
            wardCode: item.wardCode,
            wardName: item.wardName,
            latitude: item.latitude,
            longitude: item.longitude,
            imageUrl: item.imageUrl,
          });
          setSelectedWard(null);
        });
      }
    } catch (err) {
      console.error('Failed to load detail for POI', err);
    }
  }, []);

  const renderActiveView = () => {
    switch (activeView) {
      case 'admin':
        return (
          <Suspense fallback={<PanelLoadingFallback />}>
            <AdminPanel setActiveView={(view) => setActiveView(view as ActiveViewType)} />
          </Suspense>
        );
      case 'ocop':
        return (
          <Suspense fallback={<PanelLoadingFallback />}>
            <OcopPanel setActiveView={setActiveView} />
          </Suspense>
        );
      case 'science':
        return (
          <Suspense fallback={<PanelLoadingFallback />}>
            <SciencePanel setActiveView={setActiveView} />
          </Suspense>
        );
      case 'agriculture':
        return (
          <Suspense fallback={<PanelLoadingFallback />}>
            <AgriculturePanel setActiveView={setActiveView} />
          </Suspense>
        );
      case 'map':
      default:
        return (
          <GisMap
            layers={layers}
            geoJsonData={geoJsonData}
            provinceGeoJson={provinceGeoJson}
            ocopGeoJson={ocopGeoJson}
            scienceGeoJson={scienceGeoJson}
            agricultureGeoJson={agricultureGeoJson}
            selectedWard={selectedWard}
            setSelectedWard={(ward) => {
              setSelectedWard(ward);
              if (ward) setSelectedPoi(null);
            }}
            selectedPoi={selectedPoi}
            radiusSearchState={radiusSearchState}
            isPickingCenter={isPickingCenter}
            onMapCenterPicked={handleMapCenterPicked}
            onSelectDetail={handleSelectPoiDetail}
          />
        );
    }
  };

  return (
    <div className="w-full h-screen relative bg-white overflow-hidden font-sans text-neutral-900 select-none">
      {/* 1. VIEW PORT ROUTER */}
      <div className="absolute inset-0 z-0 bg-neutral-100 flex items-center justify-center">
        {isWardsLoading && activeView === 'map' && (
          <div className="absolute inset-0 bg-white/90 z-30 flex flex-col items-center justify-center space-y-4">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500"></div>
            <span className="text-sm font-semibold text-neutral-600">Đang tải bản đồ địa giới Gia Lai...</span>
          </div>
        )}

        {renderActiveView()}
      </div>

      {/* 2. FLOATING OVERLAYS (Only visible on Map View) */}
      {activeView === 'map' && (
        <>
          {/* Top Left Search - remounts using key to sync input value */}
          <MapSearch
            key={selectedWard?.properties.code || 'empty'}
            geoJsonData={geoJsonData}
            selectedWard={selectedWard}
            setSelectedWard={(ward) => {
              setSelectedWard(ward);
              if (ward) setSelectedPoi(null);
            }}
          />

          {/* Top Middle Radius Search Control */}
          <RadiusSearchControl
            radiusSearchState={radiusSearchState}
            setRadiusSearchState={setRadiusSearchState}
            isPickingCenter={isPickingCenter}
            setIsPickingCenter={setIsPickingCenter}
            selectedPoiId={selectedPoi?.id}
            onSelectDetail={handleSelectPoiDetail}
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
              <Map size={32} className="stroke-[2]" />
            )}
          </button>

          {/* Bottom Right Stats Board */}
          <StatsBoard
            geoJsonData={geoJsonData}
            ocopCount={ocopGeoJson?.features?.length || 0}
            scienceCount={scienceGeoJson?.features?.length || 0}
            agricultureCount={agricultureGeoJson?.features?.length || 0}
          />

          {/* Right Sidebar Details Panel */}
          <DetailsPanel
            selectedWard={selectedWard}
            setSelectedWard={setSelectedWard}
            selectedPoi={selectedPoi}
            setSelectedPoi={setSelectedPoi}
          />
        </>
      )}

      {/* Profile Card & Dropdown (Visible on all views for header navigation) */}
      <ProfileCard />
    </div>
  );
};

export default Home;