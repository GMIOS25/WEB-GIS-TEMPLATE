import React, { useEffect, useRef, useCallback } from 'react';
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

type MarkerVisualState = 'selected' | 'highlighted' | 'normal';

const resolveState = (id: number, selectedPoiId: number | null, highlightedIds: number[]): MarkerVisualState => {
  if (selectedPoiId === id) return 'selected';
  if (highlightedIds.includes(id)) return 'highlighted';
  return 'normal';
};

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

  // id -> marker registry + id -> last-applied visual state, so a selection/highlight
  // change only ever touches the 1-2 markers whose look actually changed instead of
  // tearing down and rebuilding every marker in the layer. This is the same registry
  // pattern already used for ward polygons above, applied here to the POI layer,
  // which is what was causing the "khựng" on marker click when a module has a lot
  // of points (every click used to rebuild the whole cluster + every popup's DOM).
  const markerRegistryRef = useRef<Map<number, L.Marker>>(new Map());
  const markerStateRef = useRef<Map<number, MarkerVisualState>>(new Map());

  // Refs so the (re)build effect below can read the *current* selection/highlight
  // state when it runs (e.g. data refetched while a POI is selected) without
  // having to list them as dependencies — that dependency is exactly what forced
  // a full rebuild on every click before.
  const selectedPoiIdRef = useRef(selectedPoiId);
  const highlightedIdsRef = useRef(highlightedIds);
  useEffect(() => {
    selectedPoiIdRef.current = selectedPoiId;
  }, [selectedPoiId]);
  useEffect(() => {
    highlightedIdsRef.current = highlightedIds;
  }, [highlightedIds]);

  // Store onSelectDetail in a ref so Leaflet event listeners always have the latest reference
  const onSelectDetailRef = useRef(onSelectDetail);
  useEffect(() => {
    onSelectDetailRef.current = onSelectDetail;
  }, [onSelectDetail]);

  // Pure icon builder, shared by initial marker creation AND incremental restyling
  // below, so both paths always render the exact same markup for a given state.
  const buildIcon = useCallback((state: MarkerVisualState) => {
    let markerSize: number;
    let markerHtml: string;

    if (state === 'selected') {
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
    } else if (state === 'highlighted') {
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
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
          cursor: pointer;
        "></div>
      `;
    }

    return L.divIcon({
      html: markerHtml,
      className: `custom-marker-${moduleType}${state === 'selected' ? ' poi-marker-selected' : ''}`,
      iconSize: L.point(markerSize, markerSize),
      iconAnchor: L.point(markerSize / 2, markerSize / 2),
    });
  }, [color, moduleType]);

  // Popup content is now built lazily: this factory is only invoked by Leaflet the
  // moment a popup actually opens (see bindPopup below), instead of eagerly building
  // an HTML string + DOM node + querySelector + addEventListener for every single
  // POI up front, most of which are never clicked in a given session.
  const buildPopupContent = useCallback((feature: PoiGeoJsonFeature, marker: L.Marker) => {
    const props = feature.properties;
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
              <img src="${props.imageUrl}" alt="${props.name}" style="width: 100%; height: 80px; object-fit: cover;" loading="lazy" />
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

    return popupContainer;
  }, [color, moduleLabel, moduleType]);

  // Effect A — (re)builds the cluster group and every marker in it. Only runs when
  // the underlying DATA (or module identity/color) changes: a fresh fetch, the
  // layer being toggled on, or the module switching color. It intentionally does
  // NOT depend on selectedPoiId/highlightedIds — see Effect B below, which is what
  // used to be folded into this same effect and forced a full rebuild on every
  // marker click/hover.
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
    markerRegistryRef.current = new Map();
    markerStateRef.current = new Map();

    if (!enabled || !data || !data.features || data.features.length === 0) {
      return;
    }

    // 2. Initialize MarkerClusterGroup with customized cluster icons matching layer color
    const clusterGroup = L.markerClusterGroup({
      showCoverageOnHover: false,
      maxClusterRadius: 45,
      spiderfyOnMaxZoom: true,
      zoomToBoundsOnClick: true,
      // Spreads marker insertion across animation frames (chunkInterval/chunkDelay,
      // library defaults) instead of adding hundreds of markers to the map in a
      // single synchronous pass. This is what removes the main-thread freeze on
      // first load / on data refetch for a module with a lot of POIs — it trades
      // a slightly slower "markers finish appearing" for a page that never stops
      // responding to input while they do.
      chunkedLoading: true,
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
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
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
      const state = resolveState(props.id, selectedPoiIdRef.current, highlightedIdsRef.current);

      const marker = L.marker([lat, lng], {
        icon: buildIcon(state),
        zIndexOffset: state === 'selected' ? 1000 : state === 'highlighted' ? 500 : 0,
      });

      // Popup content is generated on first open only (see buildPopupContent above).
      marker.bindPopup(() => buildPopupContent(feature, marker), {
        closeButton: true,
        className: 'custom-poi-popup',
      });

      markerRegistryRef.current.set(props.id, marker);
      markerStateRef.current.set(props.id, state);
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
  }, [map, enabled, data, color, moduleType, buildIcon, buildPopupContent]);

  // Effect B — the actual selection/highlight handler. Instead of rebuilding
  // anything, it diffs the new state against the last-applied state per marker
  // and only calls setIcon/setZIndexOffset on the markers that actually flipped
  // (almost always just 1-2 markers: the previously selected one and the newly
  // selected one). This runs on every click/hover, so it has to stay O(changed
  // markers), not O(total markers).
  useEffect(() => {
    const registry = markerRegistryRef.current;
    if (registry.size === 0) return;

    const prevStates = markerStateRef.current;
    const nextStates = new Map<number, MarkerVisualState>();

    registry.forEach((marker, id) => {
      const state = resolveState(id, selectedPoiId, highlightedIds);
      nextStates.set(id, state);
      if (prevStates.get(id) !== state) {
        marker.setIcon(buildIcon(state));
        marker.setZIndexOffset(state === 'selected' ? 1000 : state === 'highlighted' ? 500 : 0);
      }
    });

    markerStateRef.current = nextStates;
  }, [selectedPoiId, highlightedIds, buildIcon]);

  return null;
};
