const BACKEND_ASSET_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function toBackendAssetUrl(assetUrl) {
  if (!assetUrl) {
    return "";
  }

  if (/^(https?:|blob:|data:)/.test(assetUrl)) {
    return assetUrl;
  }

  let path = assetUrl;
  if (path.startsWith("speaking/")) {
    path = `/uploads/${path}`;
  } else if (path.startsWith("uploads/")) {
    path = `/${path}`;
  } else if (!path.startsWith("/")) {
    path = `/${path}`;
  }

  if (!BACKEND_ASSET_BASE_URL) {
    return path;
  }

  return `${BACKEND_ASSET_BASE_URL.replace(/\/$/, "")}${path}`;
}
