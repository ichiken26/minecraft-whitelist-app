#!/usr/bin/env bash
set -Eeuo pipefail
umask 027
DATA=/srv/minecraft-container-test
STATE="$DATA/plugins/KokageWhitelist"
PLUGINS="$DATA/plugins"
REQ="$STATE/update-request"
SERVICE=mc-container-test.service
BACKUPS=/var/backups/minecraft-plugin-updates
mkdir -p "$STATE" "$BACKUPS"
[[ -f "$REQ" ]] || exit 0
rm -f -- "$REQ" # consume request BEFORE running; no repeat after failure
exec 9>/run/lock/kokage-geyser-update.lock
flock -n 9 || exit 1
stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup="$BACKUPS/$stamp"
tmp=$(mktemp -d)
stopped=0
installed=0
write_status() {
  printf '{"status":"%s","at":"%s"}\n' "$1" "$(date -u +%FT%TZ)" > "$STATE/update-status.json.tmp"
  mv -f "$STATE/update-status.json.tmp" "$STATE/update-status.json"
  chmod 644 "$STATE/update-status.json"
}
cleanup() { rm -rf -- "$tmp"; }
rollback() {
  local code=$?
  trap - ERR
  if (( installed )) && [[ -d "$backup" ]]; then
    cp -a "$backup/Geyser-Spigot.jar" "$backup/floodgate-spigot.jar" "$PLUGINS/"
  fi
  if (( stopped )); then systemctl start "$SERVICE" || :; fi
  write_status failed
  cleanup
  exit "$code"
}
trap rollback ERR
trap cleanup EXIT
[[ -f "$PLUGINS/Geyser-Spigot.jar" && -f "$PLUGINS/floodgate-spigot.jar" ]] || exit 1
write_status downloading
curl --fail --location --retry 3 --connect-timeout 15 --max-time 180 \
  'https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot' -o "$tmp/Geyser-Spigot.jar"
curl --fail --location --retry 3 --connect-timeout 15 --max-time 180 \
  'https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot' -o "$tmp/floodgate-spigot.jar"
for jar in Geyser-Spigot.jar floodgate-spigot.jar; do
  unzip -tqq "$tmp/$jar" >/dev/null
  [[ -s "$tmp/$jar" ]]
done
# Keep an immutable snapshot of the live world as a separate existing backup policy.
mkdir -m 700 "$backup"
cp -a "$PLUGINS/Geyser-Spigot.jar" "$PLUGINS/floodgate-spigot.jar" "$backup/"
write_status installing
systemctl stop "$SERVICE"
stopped=1
for jar in Geyser-Spigot.jar floodgate-spigot.jar; do
  install -o minecraft -g minecraft -m 644 "$tmp/$jar" "$PLUGINS/$jar"
done
installed=1
systemctl start "$SERVICE"
stopped=0
systemctl is-active --quiet "$SERVICE"
rm -f "$STATE/update-notice"
write_status completed
