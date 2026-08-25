import React, { useState, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import { MapPin } from 'lucide-react';

import { GIA_LAI_CENTER } from '../../../config/gisConstants';

interface MapPickerProps {
  latitude: string;
  longitude: string;
  onCoordinatesChange: (lat: string, lng: string) => void;
  accentColor?: string;
}

// Sub-component to handle map click events
const LocationMarker: React.FC<{
  position: [number, number] | null;
  onCoordinatesChange: (lat: string, lng: string) => void;
  accentColor: string;
}> = ({ position, onCoordinatesChange, accentColor }) => {
  const map = useMapEvents({
    click(e) {
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;
      onCoordinatesChange(lat.toFixed(6), lng.toFixed(6));
      map.flyTo(e.latlng, map.getZoom());
    },
  });

  const markerIcon = L.divIcon({
    html: `
      <div style="
        width: 22px;
        height: 22px;
        background-color: ${accentColor};
        border: 2.5px solid #FFFFFF;
        border-radius: 50%;
        box-shadow: 0 2px 6px rgba(0,0,0,0.35), 0 0 0 4px ${accentColor}40;
      "></div>
    `,
    className: 'custom-picker-marker',
    iconSize: L.point(22, 22),
    iconAnchor: L.point(11, 11),
  });

  return position ? <Marker position={position} icon={markerIcon} /> : null;
};

export const MapPicker: React.FC<MapPickerProps> = ({
  latitude,
  longitude,
  onCoordinatesChange,
  accentColor = '#F97316',
}) => {
  const [isOpen, setIsOpen] = useState(false);

  // Derive position directly from lat/lng props without setState in effect
  const position = useMemo<[number, number] | null>(() => {
    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    if (!isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
      return [lat, lng];
    }
    return null;
  }, [latitude, longitude]);

  const defaultCenter: [number, number] = position || GIA_LAI_CENTER;

  return (
    <div className="space-y-2">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-center space-x-2 py-2 px-3 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-xl text-xs font-semibold transition-colors border border-neutral-200 cursor-pointer"
      >
        <MapPin size={14} style={{ color: accentColor }} />
        <span>{isOpen ? 'Ẩn bản đồ chọn tọa độ' : 'Chọn vị trí trực tiếp trên bản đồ'}</span>
      </button>

      {isOpen && (
        <div className="border border-neutral-300 rounded-xl overflow-hidden shadow-inner h-[220px] relative">
          <MapContainer
            center={defaultCenter}
            zoom={position ? 12 : 9}
            minZoom={6}
            maxZoom={18}
            style={{ height: '100%', width: '100%' }}
            attributionControl={false}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <LocationMarker
              position={position}
              onCoordinatesChange={onCoordinatesChange}
              accentColor={accentColor}
            />
          </MapContainer>
          <div className="absolute bottom-2 left-2 right-2 bg-white/90 backdrop-blur-xs px-2.5 py-1 rounded-md text-[11px] text-neutral-600 z-1000 shadow-xs flex items-center justify-between pointer-events-none">
            <span>💡 Click vào bản đồ để chọn tọa độ</span>
            {position && (
              <span className="font-mono font-semibold text-neutral-800">
                {position[0].toFixed(5)}, {position[1].toFixed(5)}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
export default MapPicker;
