import React, { useEffect, useRef, useCallback, useMemo } from 'react';
import { MapContainer, TileLayer, GeoJSON, useMap } from 'react-leaflet';
import L from 'leaflet';

export interface GeoJsonFeature {
  type: string;
  properties: {
    code: string;
    name: string;
    fullName?: string;
    areaKm2?: string | number;
  };
  geometry: {
    type: string;
    coordinates: number[][][] | number[][][][];
  };
}

export interface GeoJsonData {
  type: string;
  features: GeoJsonFeature[];
}

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

interface GisMapProps {
  layers: {
    province: boolean;
    commune: boolean;
  };
  geoJsonData: GeoJsonData | null;
  provinceGeoJson: unknown;
  selectedWard: GeoJsonFeature | null;
  setSelectedWard: (ward: GeoJsonFeature | null) => void;
}

const GisMap: React.FC<GisMapProps> = ({
  layers,
  geoJsonData,
  provinceGeoJson,
  selectedWard,
  setSelectedWard,
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

  return (
    <MapContainer
      center={[13.883358, 108.542896]}
      zoom={9}
      scrollWheelZoom={true}
      zoomControl={false}
      className="w-full h-full"
      // Canvas renderer draws every polygon into a single <canvas> element instead of
      // one <path> DOM node per feature (the default SVG renderer). For a layer with
      // hundreds/thousands of ward polygons this is what actually removes the multi
      // second main-thread freeze on first load and the stutter on re-styling, since
      // there's no per-feature DOM insertion/reflow cost.
      preferCanvas={true}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
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

      <MapController selectedWard={selectedWard} />
    </MapContainer>
  );
};

export default React.memo(GisMap);