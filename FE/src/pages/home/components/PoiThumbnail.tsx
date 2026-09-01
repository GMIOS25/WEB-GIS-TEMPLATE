import React, { useState } from 'react';
import { resolveImageUrl } from '../../../utils/media';

interface PoiThumbnailProps {
  imageUrl?: string | null;
  name: string;
  /** Tailwind size classes, e.g. "w-11 h-11". Applied to both the image and the fallback. */
  sizeClassName: string;
  /** Tailwind background/text/border classes for the fallback initial, e.g. "bg-orange-50 text-orange-600 border-orange-100". */
  fallbackClassName: string;
}

/**
 * Thumbnail for a POI list row (OCOP / Science / Agriculture panels).
 *
 * Tries the resolved image URL first; if it's missing, or fails to load
 * (wrong/stale path, file removed on disk, etc.), it falls back to the same
 * colored-initial placeholder each panel already used for "no image" —
 * instead of leaving the browser's native broken-image icon in the row.
 *
 * Keyed on the resolved URL so that a corrected imageUrl (e.g. an admin
 * fixes a bad path and this row re-renders) mounts a fresh load attempt
 * instead of getting stuck on a stale "failed" result from the old URL.
 */
const PoiThumbnail: React.FC<PoiThumbnailProps> = ({ imageUrl, name, sizeClassName, fallbackClassName }) => {
  const resolvedUrl = resolveImageUrl(imageUrl);
  return (
    <PoiThumbnailImage
      key={resolvedUrl ?? 'none'}
      resolvedUrl={resolvedUrl}
      name={name}
      sizeClassName={sizeClassName}
      fallbackClassName={fallbackClassName}
    />
  );
};

interface PoiThumbnailImageProps {
  resolvedUrl?: string;
  name: string;
  sizeClassName: string;
  fallbackClassName: string;
}

const PoiThumbnailImage: React.FC<PoiThumbnailImageProps> = ({
  resolvedUrl,
  name,
  sizeClassName,
  fallbackClassName,
}) => {
  const [failed, setFailed] = useState(false);

  if (!resolvedUrl || failed) {
    return (
      <div
        className={`${sizeClassName} rounded-lg flex items-center justify-center text-xs font-bold border shrink-0 ${fallbackClassName}`}
      >
        {name.charAt(0)}
      </div>
    );
  }

  return (
    <img
      src={resolvedUrl}
      alt={name}
      className={`${sizeClassName} rounded-lg object-cover border border-neutral-200 shrink-0`}
      onError={() => setFailed(true)}
    />
  );
};

export default PoiThumbnail;
