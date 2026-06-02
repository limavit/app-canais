#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Arquivo .env criado a partir de .env.example."
fi

./scripts/compose.sh up -d --build

echo
echo "Containers iniciados."
echo "Backend: http://localhost:8080/api/health"
echo "PostgreSQL: localhost:5432"
