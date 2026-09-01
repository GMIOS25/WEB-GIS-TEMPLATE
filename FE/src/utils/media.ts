/**
 * Resolves a POI `imageUrl` value into something an <img src> can always load.
 *
 * Two shapes show up in `image_url` columns today:
 *  - Absolute URLs (`https://...`, `http://...`, `data:...`, protocol-relative
 *    `//...`) — e.g. a Google Maps photo link pasted in manually. These already
 *    point at a full origin, so they are returned unchanged.
 *  - App-relative paths returned by the upload endpoint (see
 *    `LocalFileStorageService#store` -> `StoredFile.publicUrl` on the backend),
 *    always in the form `/api/files/...`. These only resolve correctly when the
 *    page's own origin is the same as the backend's — true in production
 *    (Caddy + Spring Boot serve the built frontend and the API from one
 *    origin) but NOT true in local dev, where Vite (5173) and Spring Boot
 *    (8080) are different origins and `vite.config.ts` has no dev proxy for
 *    `/api`. For a relative path we explicitly prefix it with the same API
 *    base URL `axiosInstance.ts` already uses, so it resolves in both dev and
 *    prod, and regardless of which page/component renders it.
 *
 * Note: this can only resolve a path that actually matches a real backend
 * route. A value that was typed in by hand and doesn't match `/api/files/**`
 * (the only route `FileUploadController` serves uploaded files from) will
 * still 404 — that's a bad stored value, not something this helper can fix.
 */
export function resolveImageUrl(url?: string | null): string | undefined {
  if (!url) return undefined;

  const trimmed = url.trim();
  if (!trimmed) return undefined;

  const isAbsolute = /^([a-z][a-z0-9+.-]*:)?\/\//i.test(trimmed) || trimmed.startsWith('data:');
  if (isAbsolute) {
    return trimmed;
  }

  const base = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return `${base}${path}`;
}

/**
 * Minimal HTML-escaping for values interpolated into a raw `innerHTML`
 * string (used for the Leaflet popup content in `PoiMarkerClusterLayer.tsx`,
 * which isn't rendered through React/JSX). Prevents a name, ward code, or
 * image URL containing a stray quote or angle bracket from breaking the
 * markup or injecting content into the popup.
 */
export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
