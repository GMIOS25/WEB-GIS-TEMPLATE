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
docker compose exec -T db pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB" \
    > "$BACKUP_DIR/gialai_${STAMP}.dump"

# Retention: keep 7 daily + 4 weekly (Sunday) backups
find "$BACKUP_DIR" -name "gialai_*.dump" -mtime +7 ! -name "*_Sun_*" -delete
find "$BACKUP_DIR" -name "gialai_*_Sun_*.dump" -mtime +27 -delete
