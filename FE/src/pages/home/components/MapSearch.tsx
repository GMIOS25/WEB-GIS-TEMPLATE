import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Search, X, Landmark } from 'lucide-react';

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

interface MapSearchProps {
  geoJsonData: GeoJsonData | null;
  selectedWard: GeoJsonFeature | null;
  setSelectedWard: (ward: GeoJsonFeature | null) => void;
}

const MapSearch: React.FC<MapSearchProps> = ({
  geoJsonData,
  selectedWard,
  setSelectedWard,
}) => {
  const [searchQuery, setSearchQuery] = useState(
    selectedWard ? (selectedWard.properties.fullName || selectedWard.properties.name || '') : ''
  );
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const autocompleteRef = useRef<HTMLDivElement>(null);

  // Compute search results directly in render instead of using useEffect state updates
  const searchResults = useMemo(() => {
    if (!searchQuery.trim() || !geoJsonData) {
      return [];
    }
    const currentName = selectedWard?.properties.fullName || selectedWard?.properties.name || '';
    if (searchQuery === currentName) {
      return [];
    }
    return geoJsonData.features
      .filter((f) =>
        f.properties.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        f.properties.name?.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .slice(0, 5);
  }, [searchQuery, geoJsonData, selectedWard]);

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

  return (
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
          {searchResults.map((f) => (
            <button
              key={f.properties.code}
              onClick={() => {
                setSelectedWard(f);
                setSearchQuery(f.properties.fullName || f.properties.name);
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
  );
};

export default React.memo(MapSearch);
