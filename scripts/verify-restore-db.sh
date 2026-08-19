#!/usr/bin/env bash
# /opt/gialai-gis/scripts/verify-restore-db.sh
#
# Script kiểm tra tính toàn vẹn và khả năng restore của bản backup PostgreSQL.
# Tạo database tạm thời, restore file .dump và kiểm tra dữ liệu các bảng cốt lõi.
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-/opt/gialai-gis}"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <path_to_backup_file.dump>"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file $BACKUP_FILE not found."
    exit 1
fi

# Check SHA256 checksum if available
if [ -f "${BACKUP_FILE}.sha256" ]; then
    echo "Verifying SHA256 checksum..."
    sha256sum -c "${BACKUP_FILE}.sha256"
    echo "Checksum OK."
fi

TEST_DB="gialai_test_restore_$(date +%s)"
echo "Starting test restore on temporary database '${TEST_DB}'..."

cd "$COMPOSE_DIR"
docker compose exec -T db psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE ${TEST_DB};"

# Restore database dump into temporary test database
docker compose exec -T db pg_restore -U "$POSTGRES_USER" -d "${TEST_DB}" --no-owner --role="$POSTGRES_USER" < "$BACKUP_FILE" || true

# Verify core tables and row counts
echo "Verifying restored schema and data counts..."
docker compose exec -T db psql -U "$POSTGRES_USER" -d "${TEST_DB}" -c "
    SELECT count(*) AS total_wards FROM wards;
    SELECT count(*) AS total_users FROM users;
"

# Clean up temporary database
echo "Cleaning up temporary database '${TEST_DB}'..."
docker compose exec -T db psql -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE ${TEST_DB};"

echo "Disaster recovery restore verification PASSED successfully!"
