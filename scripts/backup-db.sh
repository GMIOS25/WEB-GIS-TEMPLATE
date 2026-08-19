#!/usr/bin/env bash
# /opt/gialai-gis/scripts/backup-db.sh
#
# Daily pg_dump chạy từ host qua cron (đặt ngoài app container để sống sót qua các
# lần redeploy app) - xem docs/en/DEPLOYMENT & FLEET STRATEGY.md Section 5.3.
#
# Yêu cầu: chạy từ thư mục gốc repo (nơi có docker-compose.yml) hoặc chỉnh
# COMPOSE_DIR bên dưới, và các biến POSTGRES_USER/POSTGRES_DB phải có trong môi
# trường gọi script này (vd. export từ .env trước khi cron gọi, hoặc dùng
# `env $(cat .env | grep -v '^#' | xargs) ./scripts/backup-db.sh`).
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-/opt/gialai-gis}"
# Use the C locale so Sunday is consistently named "Sun" for retention below.
STAMP=$(LC_TIME=C date +%Y%m%d_%a_%H%M%S)
BACKUP_DIR="${COMPOSE_DIR}/backups"
mkdir -p "$BACKUP_DIR"

cd "$COMPOSE_DIR"
BACKUP_FILE="${BACKUP_DIR}/gialai_${STAMP}.dump"

echo "[$(date -Iseconds)] Starting PostgreSQL database backup..."
docker compose exec -T db pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB" > "$BACKUP_FILE"

# Compute SHA256 checksum for integrity verification
sha256sum "$BACKUP_FILE" > "${BACKUP_FILE}.sha256"
echo "[$(date -Iseconds)] Backup created at ${BACKUP_FILE} (Size: $(du -h "${BACKUP_FILE}" | cut -f1))"

# Retention: keep 7 daily + 4 weekly (Sunday) backups
find "$BACKUP_DIR" -name "gialai_*.dump" -mtime +7 ! -name "*_Sun_*" -delete
find "$BACKUP_DIR" -name "gialai_*_Sun_*.dump" -mtime +27 -delete
find "$BACKUP_DIR" -name "gialai_*.sha256" -mtime +28 -delete

# ==============================================================================
# OFF-SITE REPLICATION (Hardened in Phase 2 - DEPLOYMENT & FLEET STRATEGY Section 5.3)
# ==============================================================================

# 1. S3-Compatible Object Storage (AWS S3, Cloudflare R2, MinIO, or Local VN Cloud S3)
if [ -n "${S3_BACKUP_BUCKET:-}" ]; then
    echo "[$(date -Iseconds)] Replicating backup to S3 bucket: ${S3_BACKUP_BUCKET}..."
    if command -v aws >/dev/null 2>&1; then
        S3_ENDPOINT_ARG=""
        if [ -n "${S3_ENDPOINT_URL:-}" ]; then
            S3_ENDPOINT_ARG="--endpoint-url ${S3_ENDPOINT_URL}"
        fi
        aws $S3_ENDPOINT_ARG s3 cp "$BACKUP_FILE" "s3://${S3_BACKUP_BUCKET}/db-backups/gialai_${STAMP}.dump"
        aws $S3_ENDPOINT_ARG s3 cp "${BACKUP_FILE}.sha256" "s3://${S3_BACKUP_BUCKET}/db-backups/gialai_${STAMP}.dump.sha256"
        echo "[$(date -Iseconds)] Successfully replicated to S3."
    elif command -v rclone >/dev/null 2>&1; then
        rclone copy "$BACKUP_FILE" "remote:${S3_BACKUP_BUCKET}/db-backups/"
        rclone copy "${BACKUP_FILE}.sha256" "remote:${S3_BACKUP_BUCKET}/db-backups/"
        echo "[$(date -Iseconds)] Successfully replicated via rclone."
    else
        echo "[$(date -Iseconds)] WARNING: S3_BACKUP_BUCKET configured but neither 'aws' nor 'rclone' CLI is installed."
    fi
fi

# 2. Remote Secondary VPS via Rsync over SSH
if [ -n "${OFFSITE_SSH_TARGET:-}" ]; then
    echo "[$(date -Iseconds)] Replicating backup via rsync to ${OFFSITE_SSH_TARGET}..."
    rsync -avz -e "ssh -o BatchMode=yes -o ConnectTimeout=10" \
        "$BACKUP_FILE" "${BACKUP_FILE}.sha256" \
        "${OFFSITE_SSH_TARGET}:/backups/gialai-gis/"
    echo "[$(date -Iseconds)] Successfully replicated via rsync to secondary VPS."
fi

echo "[$(date -Iseconds)] Database backup and replication completed."
