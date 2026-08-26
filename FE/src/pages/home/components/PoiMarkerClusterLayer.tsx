import React, { useEffect, useRef } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.markercluster';
import type { PoiGeoJsonData, PoiGeoJsonFeature } from '../../../types/gis';

interface PoiMarkerClusterLayerProps {
  moduleType: 'ocop' | 'science' | 'agriculture';
  moduleLabel: string;
  color: string; // e.g. '#F97316' for OCOP, '#64748B' for Science, '#6B7280' for Agriculture
  enabled: boolean;
  data?: PoiGeoJsonData | null;
  highlightedIds?: number[];
  selectedPoiId?: number | null;
  onSelectDetail?: (type: 'ocop' | 'science' | 'agriculture', id: number) => void;
}

export const PoiMarkerClusterLayer: React.FC<PoiMarkerClusterLayerProps> = ({
  moduleType,
  moduleLabel,
  color,
  enabled,
  data,
  highlightedIds = [],
  selectedPoiId = null,
  onSelectDetail,
}) => {
  const map = useMap();
  const clusterGroupRef = useRef<L.MarkerClusterGroup | null>(null);

  // Store onSelectDetail in a ref so Leaflet event listeners always have the latest reference
  const onSelectDetailRef = useRef(onSelectDetail);
  useEffect(() => {
    onSelectDetailRef.current = onSelectDetail;
  }, [onSelectDetail]);

  useEffect(() => {
    // 1. Defensively clear any existing cluster layer
    if (clusterGroupRef.current) {
      const prevCluster = clusterGroupRef.current;
      clusterGroupRef.current = null;
      if (map.hasLayer(prevCluster)) {
        map.removeLayer(prevCluster);
      }
      prevCluster.clearLayers();
    }

    if (!enabled || !data || !data.features || data.features.length === 0) {
      return;
    }

    // 2. Initialize MarkerClusterGroup with customized cluster icons matching layer color
    const clusterGroup = L.markerClusterGroup({
      showCoverageOnHover: false,
      maxClusterRadius: 45,
      spiderfyOnMaxZoom: true,
      zoomToBoundsOnClick: true,
      iconCreateFunction: (cluster) => {
        const count = cluster.getChildCount();
        const size = count < 10 ? 32 : count < 50 ? 38 : 44;
        return L.divIcon({
          html: `
            <div style="
              width: ${size}px;
              height: ${size}px;
              background-color: ${color};
              color: #FFFFFF;
              border: 2.5px solid #FFFFFF;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              font-weight: 700;
              font-size: ${count < 10 ? '12px' : '13px'};
              font-family: ui-sans-serif, system-ui, sans-serif;
              box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25), 0 0 0 2px ${color}40;
              transition: transform 0.15s ease-out;
            ">
              ${count}
            </div>
          `,
          className: `custom-cluster-${moduleType}`,
          iconSize: L.point(size, size),
        });
      },
    });

    // 3. Create single markers
    data.features.forEach((feature: PoiGeoJsonFeature) => {
      const coords = feature.geometry?.coordinates;
      if (!coords || coords.length < 2) return;

      const lng = coords[0];
      const lat = coords[1];
      const props = feature.properties;
      const isSelected = selectedPoiId === props.id;
      const isHighlighted = highlightedIds.includes(props.id);

      let markerSize = 18;
      let markerHtml = '';

      if (isSelected) {
        markerSize = 24;
        markerHtml = `
          <div style="
            width: ${markerSize}px;
            height: ${markerSize}px;
            background-color: ${color};
            border: 3px solid #FFFFFF;
            border-radius: 50%;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4), 0 0 0 3px ${color}90;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            z-index: 1000;
          ">
            <div style="width: 6px; height: 6px; background-color: #FFFFFF; border-radius: 50%;"></div>
            <!-- Water drop concentric ripple waves -->
            <div class="poi-water-ripple-ring poi-water-ripple-ring-1" style="border: 2.5px solid ${color}; background-color: ${color}18;"></div>
            <div class="poi-water-ripple-ring poi-water-ripple-ring-2" style="border: 2px solid ${color}; background-color: ${color}10;"></div>
            <div class="poi-water-ripple-ring poi-water-ripple-ring-3" style="border: 1.5px solid ${color};"></div>
          </div>
        `;
      } else if (isHighlighted) {
        markerSize = 20;
        markerHtml = `
          <div style="
            width: ${markerSize}px;
            height: ${markerSize}px;
            background-color: ${color};
            border: 2.5px solid #FFFFFF;
            border-radius: 50%;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35), 0 0 0 4px ${color}50;
            cursor: pointer;
            position: relative;
            transition: transform 0.2s ease;
          "></div>
        `;
      } else {
        markerSize = 18;
        markerHtml = `
          <div style="
            width: ${markerSize}px;
            height: ${markerSize}px;
            background-color: ${color};
            border: 2px solid #FFFFFF;
            border-radius: 50%;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
            cursor: pointer;
            transition: transform 0.15s ease;
          "></div>
        `;
      }

      const customIcon = L.divIcon({
        html: markerHtml,
        className: `custom-marker-${moduleType}${isSelected ? ' poi-marker-selected' : ''}`,
        iconSize: L.point(markerSize, markerSize),
        iconAnchor: L.point(markerSize / 2, markerSize / 2),
      });

      const marker = L.marker([lat, lng], {
        icon: customIcon,
        zIndexOffset: isSelected ? 1000 : isHighlighted ? 500 : 0,
      });

      // Build popup content
      const typeDisplay = props.productType || props.unitType || moduleLabel;
      const starsHtml = props.starRating
        ? `<div style="margin-top: 3px; color: #F59E0B; font-size: 13px; letter-spacing: 1px;">
            ${'★'.repeat(props.starRating)}${'<span style="color:#D1D5DB">☆</span>'.repeat(5 - props.starRating)}
            <span style="font-size: 11px; font-weight: 700; color: #D97706; margin-left: 2px;">(${props.starRating}★)</span>
           </div>`
        : '';

      const popupContainer = document.createElement('div');
      popupContainer.style.fontFamily = 'ui-sans-serif, system-ui, sans-serif';
      popupContainer.style.fontSize = '13px';
      popupContainer.style.lineHeight = '1.4';
      popupContainer.style.minWidth = '180px';
      popupContainer.style.maxWidth = '240px';

      popupContainer.innerHTML = `
        <div style="margin-bottom: 6px;">
          <span style="
            display: inline-block;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 6px;
            border-radius: 4px;
            background-color: ${color}20;
            color: ${color};
            margin-bottom: 4px;
          ">
            ${typeDisplay}
          </span>
          <div style="font-weight: 700; color: #111827; font-size: 14px; margin-top: 2px;">
            ${props.name}
          </div>
          ${starsHtml}
          ${
            props.wardCode
              ? `<div style="font-size: 12px; color: #6B7280; margin-top: 2px;">Mã xã: ${props.wardCode}</div>`
              : ''
          }
        </div>
        ${
          props.imageUrl
            ? `<div style="margin-bottom: 8px; border-radius: 6px; overflow: hidden; max-height: 100px;">
                <img src="${props.imageUrl}" alt="${props.name}" style="width: 100%; height: 80px; object-fit: cover;" />
               </div>`
            : ''
        }
        <button id="poi-detail-btn-${moduleType}-${props.id}" style="
          width: 100%;
          background-color: #059669;
          color: white;
          border: none;
          padding: 6px 10px;
          border-radius: 6px;
          font-weight: 600;
          font-size: 12px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
          transition: background-color 0.15s ease;
        ">
          Xem chi tiết
        </button>
      `;

      const detailBtn = popupContainer.querySelector(`#poi-detail-btn-${moduleType}-${props.id}`);
      if (detailBtn) {
        detailBtn.addEventListener('click', () => {
          marker.closePopup();
          if (onSelectDetailRef.current) {
            onSelectDetailRef.current(moduleType, props.id);
          }
        });
      }

      marker.bindPopup(popupContainer, {
        closeButton: true,
        className: 'custom-poi-popup',
      });

      clusterGroup.addLayer(marker);
    });

    map.addLayer(clusterGroup);
    clusterGroupRef.current = clusterGroup;

    // Cleanup on unmount, data change, or layer toggle
    return () => {
      clusterGroupRef.current = null;
      if (map.hasLayer(clusterGroup)) {
        map.removeLayer(clusterGroup);
      }
      clusterGroup.clearLayers();
    };
  }, [map, enabled, data, color, moduleType, moduleLabel, highlightedIds, selectedPoiId]);

  return null;
};
