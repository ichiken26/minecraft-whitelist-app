# Geyser / Floodgate update manager

Targets ONLY the systemd-nspawn server in `/srv/minecraft-container-test` (`mc-container-test.service`). Never update `/opt/minecraft`.

1. Keep a verified world backup. Install the plugin JAR into `/srv/minecraft-container-test/plugins/` and restart the container.
2. Copy `deploy/kokage-geyser-update.sh` to `/usr/local/sbin/kokage-geyser-update.sh`, owned by root with mode 0755.
3. Install `deploy/kokage-geyser-update.service` and `deploy/kokage-geyser-update.path` to `/etc/systemd/system/`.
4. `sudo systemctl daemon-reload && sudo systemctl enable --now kokage-geyser-update.path`.
5. Set `QUEUE_DIR=/srv/minecraft-container-test/plugins/KokageWhitelist` in `/etc/kokage-manager.env`. Ensure the minecraft user can write there (including under your nspawn/idmapped bind setup).
6. Build/deploy Nuxt, restart `kokage-manager.service`, and verify the web UI over Tailscale. The UI creates a request file; the privileged path unit carries out the update.
7. Confirm `journalctl -u kokage-geyser-update.service -n 100`, server plugin versions, and a real Bedrock login.

Updater fetches official GeyserMC JARs, verifies ZIP structure, backs up previous JARs, stops the *container* service, installs, and restarts. On installation failure it rolls the JARs back. Do not use it as a substitute for compatibility testing. Update detection monitors Geyser's own `logs/latest.log` output and broadcasts a reminder on every player join while a notice file exists; this is intentionally distinct from querying latest-build APIs. A failed download does not change installed JARs. The root updater writes its status into the plugin data directory.
