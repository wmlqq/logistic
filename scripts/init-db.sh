#!/usr/bin/env bash
# Initialize MySQL database and import seed data.
# Usage: ./scripts/init-db.sh [mysql_user] [mysql_password]

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
USER="${1:-root}"
PASSWORD="${2:-}"
DATABASE="mylogistic"
SEED="${ROOT}/database/seed/backup.sql"

if [[ ! -f "$SEED" ]]; then
  echo "Seed file not found: $SEED" >&2
  exit 1
fi

MYSQL=(mysql -u "$USER")
if [[ -n "$PASSWORD" ]]; then
  MYSQL+=(-p"$PASSWORD")
fi

echo "Creating database ${DATABASE}..."
"${MYSQL[@]}" -e "CREATE DATABASE IF NOT EXISTS ${DATABASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
"${MYSQL[@]}" "$DATABASE" < "$SEED"

echo "Done. Default admin: admin / admin"
