import React, { useEffect, useRef, useCallback, useMemo } from 'react';
import { MapContainer, TileLayer, GeoJSON, Circle, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';

import type { GeoJsonFeature, GeoJsonData, PoiGeoJsonData } from '../../../types/gis';
import { GIA_LAI_CENTER, DEFAULT_MAP_ZOOM } from '../../../config/gisConstants';
import { PoiMarkerClusterLayer } from './PoiMarkerClusterLayer';
import type { RadiusSearchState } from './RadiusSearchControl';
import type { SelectedPoiDetail } from './DetailsPanel';
import { FEATURE_FLAGS } from '../../../config/features';

export type { GeoJsonFeature, GeoJsonData };

const DEFAULT_STYLE = {
  fillColor: '#10b981',
  weight: 1,
  opacity: 1,
  color: '#6b7280',
  fillOpacity: 0.08,
};

const HOVER_STYLE = {
  weight: 3,
  color: '#059669',
  fillColor: '#6ee7b7',
  fillOpacity: 0.35,
};

const SELECTED_STYLE = {
  fillColor: '#a7f3d0',
  weight: 3,
  opacity: 1,
  color: '#059669',
  fillOpacity: 0.3,
};

// Map controller sub-component to handle programmatically flying to selected ward boundaries
const MapController: React.FC<{ selectedWard: GeoJsonFeature | null }> = ({ selectedWard }) => {
  const map = useMap();
  useEffect(() => {
    if (selectedWard && selectedWard.geometry) {
      const geojsonFeature = selectedWard as unknown as Parameters<typeof L.geoJSON>[0];
      const layer = L.geoJSON(geojsonFeature);
      const bounds = layer.getBounds();
      if (bounds.isValid()) {
        map.flyToBounds(bounds, { maxZoom: 13, duration: 1.2 });
      }
    }
  }, [selectedWard, map]);
  return null;
};

// Sub-component to smoothly center the map on selected POI point
const PoiFocusController: React.FC<{ selectedPoi: SelectedPoiDetail | null }> = ({ selectedPoi }) => {
  const map = useMap();
  useEffect(() => {
    if (selectedPoi && selectedPoi.latitude && selectedPoi.longitude) {
      map.flyTo([selectedPoi.latitude, selectedPoi.longitude], Math.max(map.getZoom(), 14), {
        duration: 1.0,
      });
    }
  }, [selectedPoi, map]);
  return null;
};

// Sub-component to capture map clicks for radius search center picking
const MapClickHandler: React.FC<{
  isPickingCenter?: boolean;
  onMapCenterPicked?: (lat: number, lng: number) => void;
}> = ({ isPickingCenter, onMapCenterPicked }) => {
  useMapEvents({
    click(e) {
      if (isPickingCenter && onMapCenterPicked) {
        onMapCenterPicked(e.latlng.lat, e.latlng.lng);
      }
    },
  });
  return null;
};

interface GisMapProps {
  layers: {
    province: boolean;
    commune: boolean;
    ocop: boolean;
    science: boolean;
    agriculture: boolean;
  };
  geoJsonData: GeoJsonData | null;
  provinceGeoJson: unknown;
  ocopGeoJson?: PoiGeoJsonData | null;
  scienceGeoJson?: PoiGeoJsonData | null;
  agricultureGeoJson?: PoiGeoJsonData | null;
  selectedWard: GeoJsonFeature | null;
  setSelectedWard: (ward: GeoJsonFeature | null) => void;
  selectedPoi?: SelectedPoiDetail | null;
  radiusSearchState?: RadiusSearchState | null;
  isPickingCenter?: boolean;
  onMapCenterPicked?: (lat: number, lng: number) => void;
  onSelectDetail?: (type: 'ocop' | 'science' | 'agriculture', id: number) => void;
}

const GisMap: React.FC<GisMapProps> = ({
  layers,
  geoJsonData,
  provinceGeoJson,
  ocopGeoJson,
  scienceGeoJson,
  agricultureGeoJson,
  selectedWard,
  setSelectedWard,
  selectedPoi = null,
  radiusSearchState,
  isPickingCenter,
  onMapCenterPicked,
  onSelectDetail,
}) => {
  // Use ref to avoid stale closures in Leaflet event listeners
  const selectedWardRef = useRef(selectedWard);
  useEffect(() => {
    selectedWardRef.current = selectedWard;
  }, [selectedWard]);

  // Registry of every rendered ward layer, keyed by ward code, so selection changes
  // only ever touch the 1-2 layers that actually changed instead of re-styling the
  // whole FeatureCollection (which was the cause of the "khựng" / stutter on click).
  const layerRegistryRef = useRef<Map<string, L.Path>>(new Map());
  // Tracks which layer is currently hovered so we can defensively clear it. This guards
  // against the classic Leaflet "ghost hover" issue where, on fast mouse movement,
  // bringToFront() reorders the underlying DOM node and the browser can end up not
  // firing mouseout on the previously-hovered layer before mouseover fires on the next.
  const hoveredLayerRef = useRef<{ code: string; layer: L.Path } | null>(null);

  const applyBaseStyle = useCallback((layer: L.Path, code: string) => {
    const isSelected = selectedWardRef.current?.properties.code === code;
    layer.setStyle(isSelected ? SELECTED_STYLE : DEFAULT_STYLE);
  }, []);

  // Leaflet Layer Interactive Styles
  const onEachFeature = useCallback((feature: unknown, layer: L.Layer) => {
    const f = feature as GeoJsonFeature;
    const code = f.properties.code;
    layerRegistryRef.current.set(code, layer as L.Path);

    layer.on({
      mouseover: (e: L.LeafletMouseEvent) => {
        const hoverLayer = e.target as L.Path;

        // If a different layer was left in a "hovered" state (missed mouseout because
        // of fast pointer movement), reset it right now instead of trusting the event
        // order. This is what actually eliminates the ghost-hover artifact.
        const prevHovered = hoveredLayerRef.current;
        if (prevHovered && prevHovered.code !== code) {
          applyBaseStyle(prevHovered.layer, prevHovered.code);
        }
        hoveredLayerRef.current = { code, layer: hoverLayer };

        hoverLayer.setStyle(HOVER_STYLE);
        if (!L.Browser.ie && !L.Browser.opera && !L.Browser.edge) {
          hoverLayer.bringToFront();
        }
      },
      mouseout: (e: L.LeafletMouseEvent) => {
        const hoverLayer = e.target as L.Path;
        if (hoveredLayerRef.current?.code === code) {
          hoveredLayerRef.current = null;
        }
        applyBaseStyle(hoverLayer, code);
      },
      click: () => {
        setSelectedWard(f);
      },
    });
  }, [setSelectedWard, applyBaseStyle]);

  // Style function only runs once per layer at creation time now (stable reference,
  // no dependency on selectedWard) — selection highlighting below is applied directly
  // and imperatively via the layer registry instead of forcing react-leaflet to
  // re-run `style` across every feature in the collection on each click.
  const getFeatureStyle = useCallback((feature?: unknown) => {
    const f = feature as GeoJsonFeature | undefined;
    const isSelected = selectedWardRef.current?.properties.code === f?.properties.code;
    return isSelected ? SELECTED_STYLE : DEFAULT_STYLE;
  }, []);

  // Imperatively restyle only the previously selected + newly selected wards (O(1))
  // instead of relying on the GeoJSON `style` prop, which would restyle every feature.
  const prevSelectedCodeRef = useRef<string | null>(null);
  useEffect(() => {
    const registry = layerRegistryRef.current;
    const prevCode = prevSelectedCodeRef.current;
    const nextCode = selectedWard?.properties.code ?? null;

    if (prevCode && prevCode !== nextCode) {
      const prevLayer = registry.get(prevCode);
      if (prevLayer) applyBaseStyle(prevLayer, prevCode);
    }
    if (nextCode) {
      const nextLayer = registry.get(nextCode);
      if (nextLayer) nextLayer.setStyle(SELECTED_STYLE);
    }
    prevSelectedCodeRef.current = nextCode;
  }, [selectedWard, applyBaseStyle]);

  // Reset the layer registry whenever the underlying dataset changes (new GeoJSON
  // instance mounted), so stale layer references aren't kept around.
  const wardsKey = useMemo(() => geoJsonData?.features?.length ?? 0, [geoJsonData]);
  useEffect(() => {
    layerRegistryRef.current = new Map();
    hoveredLayerRef.current = null;
    prevSelectedCodeRef.current = null;
  }, [wardsKey]);

  // A wider padding means Leaflet keeps a bigger off-screen buffer drawn on the
  // canvas, so panning a bit outside the visible viewport doesn't immediately force
  // a full redraw of every polygon — this is what removes the stutter while dragging
  // the map. Default padding is only 0.1 (10% of viewport); 1 means a full extra
  // viewport's worth of buffer in every direction.
  const canvasRenderer = useMemo(() => L.canvas({ padding: 1 }), []);

  return (
    <MapContainer
      center={GIA_LAI_CENTER}
      zoom={DEFAULT_MAP_ZOOM}
      minZoom={6}
      maxZoom={18}
      scrollWheelZoom={true}
      zoomControl={false}
      className="w-full h-full"
      // A shared, padded Canvas renderer as the map's default renderer for all vector
      // layers. Canvas draws every polygon into a single <canvas> element instead of
      // one <path> DOM node per feature (the default SVG renderer) — for a layer with
      // hundreds/thousands of ward polygons this is what removes the multi-second
      // main-thread freeze on first load and the stutter on re-styling, since there's
      // no per-feature DOM insertion/reflow cost. The wider padding (default is 0.1,
      // i.e. 10% of the viewport) keeps a bigger off-screen buffer already drawn, so
      // panning a bit outside the visible viewport doesn't immediately force a full
      // redraw of every polygon — this is what removes the stutter while dragging.
      renderer={canvasRenderer}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        // Keep more previously-loaded tiles around the edges instead of discarding
        // them immediately, so panning/zooming shows fewer blank/gray tiles while
        // new ones load.
        keepBuffer={4}
      />

      {/* Wards Boundary layer */}
      {layers.commune && geoJsonData && (
        <GeoJSON
          key={wardsKey}
          data={geoJsonData as unknown as Parameters<typeof GeoJSON>[0]['data']}
          style={getFeatureStyle}
          onEachFeature={onEachFeature}
        />
      )}

      {/* Province Outline boundary */}
      {layers.province && !!provinceGeoJson && (
        <GeoJSON
          data={provinceGeoJson as unknown as Parameters<typeof GeoJSON>[0]['data']}
          style={{
            fillColor: 'transparent',
            weight: 3,
            color: '#047857',
            opacity: 1,
            fillOpacity: 0,
          }}
          interactive={false}
        />
      )}

      {/* Feature POI Marker Cluster Layers */}
      {FEATURE_FLAGS.ocop && (
        <PoiMarkerClusterLayer
          moduleType="ocop"
          moduleLabel="Sản phẩm OCOP"
          color="#F97316"
          enabled={layers.ocop}
          data={ocopGeoJson}
          highlightedIds={radiusSearchState?.module === 'ocop' ? radiusSearchState.resultIds : []}
          selectedPoiId={selectedPoi?.moduleType === 'ocop' ? selectedPoi.id : null}
          onSelectDetail={onSelectDetail}
        />
      )}

      {FEATURE_FLAGS.science && (
        <PoiMarkerClusterLayer
          moduleType="science"
          moduleLabel="Khoa học & CN"
          color="#64748B"
          enabled={layers.science}
          data={scienceGeoJson}
          highlightedIds={radiusSearchState?.module === 'science' ? radiusSearchState.resultIds : []}
          selectedPoiId={selectedPoi?.moduleType === 'science' ? selectedPoi.id : null}
          onSelectDetail={onSelectDetail}
        />
      )}

      {FEATURE_FLAGS.agriculture && (
        <PoiMarkerClusterLayer
          moduleType="agriculture"
          moduleLabel="Nông nghiệp"
          color="#6B7280"
          enabled={layers.agriculture}
          data={agricultureGeoJson}
          highlightedIds={radiusSearchState?.module === 'agriculture' ? radiusSearchState.resultIds : []}
          selectedPoiId={selectedPoi?.moduleType === 'agriculture' ? selectedPoi.id : null}
          onSelectDetail={onSelectDetail}
        />
      )}

      {/* Radius Search Circle & Center Pin */}
      {radiusSearchState?.center && (
        <>
          <Circle
            center={radiusSearchState.center}
            radius={radiusSearchState.radiusKm * 1000}
            pathOptions={{
              color: '#059669',
              fillColor: '#10B981',
              fillOpacity: 0.15,
              weight: 2,
              dashArray: '6, 6',
            }}
          />
          <Marker
            position={radiusSearchState.center}
            icon={L.divIcon({
              html: `
                <div style="
                  width: 14px;
                  height: 14px;
                  background-color: #059669;
                  border: 2.5px solid #FFFFFF;
                  border-radius: 50%;
                  box-shadow: 0 0 0 4px rgba(5, 150, 105, 0.35);
                "></div>
              `,
              className: 'radius-center-marker',
              iconSize: L.point(14, 14),
              iconAnchor: L.point(7, 7),
            })}
          />
        </>
      )}

      <MapClickHandler
        isPickingCenter={isPickingCenter}
        onMapCenterPicked={onMapCenterPicked}
      />

      <MapController selectedWard={selectedWard} />
      <PoiFocusController selectedPoi={selectedPoi} />
    </MapContainer>
  );
};

export default React.memo(GisMap);