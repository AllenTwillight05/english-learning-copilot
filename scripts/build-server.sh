#!/usr/bin/env bash

# Build deployable frontend and backend artifacts from a checked-out repository.
# This script deliberately does not pull Git changes, publish files, or restart
# services. Run it as the `deploy` user on the server after reviewing `main`.

set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_root/frontend"
backend_dir="$repo_root/backend"

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

require_supported_node() {
  local version major minor patch
  version="$(node --version | sed 's/^v//')"
  IFS='.' read -r major minor patch <<<"$version"

  if (( major < 20 || (major == 20 && minor < 19) )); then
    echo "Node.js $version is unsupported. Vite 8 requires Node.js >= 20.19 or >= 22.12." >&2
    exit 1
  fi
}

require_command git
require_command node
require_command npm
require_command java
require_command mvn
require_supported_node

# Production defaults: real authentication API, mock data for unfinished modules.
# Callers can override any VITE_* variable when a module's backend is ready.
export VITE_API_MODE="${VITE_API_MODE:-mixed}"
export VITE_API_BASE_URL="${VITE_API_BASE_URL:-}"
export VITE_AUTH_API_MODE="${VITE_AUTH_API_MODE:-http}"
export VITE_DASHBOARD_API_MODE="${VITE_DASHBOARD_API_MODE:-mock}"
export VITE_SPEAKING_API_MODE="${VITE_SPEAKING_API_MODE:-mock}"
export VITE_VOCABULARY_API_MODE="${VITE_VOCABULARY_API_MODE:-mock}"
export VITE_GRAMMAR_API_MODE="${VITE_GRAMMAR_API_MODE:-mock}"
export VITE_PROFILE_API_MODE="${VITE_PROFILE_API_MODE:-mock}"

echo "Building commit $(git -C "$repo_root" rev-parse --short HEAD) from $repo_root"
echo "Frontend API mode: $VITE_API_MODE (auth: $VITE_AUTH_API_MODE, vocabulary: $VITE_VOCABULARY_API_MODE)"

echo "==> Building backend and running its tests"
(
  cd "$backend_dir"
  mvn -B clean package
)

echo "==> Installing locked frontend dependencies"
(
  cd "$frontend_dir"
  npm ci
)

echo "==> Running frontend unit tests"
(
  cd "$frontend_dir"
  npm run test:run
)

echo "==> Building frontend"
(
  cd "$frontend_dir"
  npm run build
)

echo
echo "Build completed. Artifacts:"
echo "  frontend: $frontend_dir/dist"
echo "  backend:  $backend_dir/target/backend-0.1.0-SNAPSHOT.jar"
