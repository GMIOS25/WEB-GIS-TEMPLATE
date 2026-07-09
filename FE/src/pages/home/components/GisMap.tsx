import React, { useEffect, useRef, useCallback } from 'react';
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

  // Leaflet Layer Interactive Styles
  const onEachFeature = useCallback((feature: unknown, layer: L.Layer) => {
    const f = feature as GeoJsonFeature;
    layer.on({
      mouseover: (e: L.LeafletMouseEvent) => {
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
      mouseout: (e: L.LeafletMouseEvent) => {
        const hoverLayer = e.target;
        const currentSelectedWard = selectedWardRef.current;
        const isSelected = currentSelectedWard && currentSelectedWard.properties.code === f.properties.code;
        hoverLayer.setStyle({
          weight: isSelected ? 3 : 1,
          color: isSelected ? '#10b981' : '#6b7280',
          fillColor: isSelected ? '#a7f3d0' : '#10b981',
          fillOpacity: isSelected ? 0.3 : 0.08,
        });
      },
      click: () => {
        setSelectedWard(f);
      },
    });
  }, [setSelectedWard]);

  // Memoize style function to prevent unnecessary re-styling calculations on other props changes
  const getFeatureStyle = useCallback((feature?: unknown) => {
    const f = feature as GeoJsonFeature | undefined;
    const isSelected = selectedWard && selectedWard.properties.code === f?.properties.code;
    return {
      fillColor: isSelected ? '#a7f3d0' : '#10b981',
      weight: isSelected ? 3 : 1,
      opacity: 1,
      color: isSelected ? '#059669' : '#6b7280',
      fillOpacity: isSelected ? 0.3 : 0.08,
    };
  }, [selectedWard]);

  return (
    <MapContainer
      center={[13.883358, 108.542896]}
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
