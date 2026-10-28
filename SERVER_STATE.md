# Minecraft 運用記録（2026-10-29）

## 稼働環境
- 稼働サービス: `mc-container-test.service`（enabled、systemd-nspawn / namespaces / cgroups）。
- 起動処理: `/usr/local/sbin/mc-container-start.sh`、ネットワーク設定: `/usr/local/sbin/mc-container-net.sh`。
- コンテナ定義: `/var/lib/machines/mc-userns-lab`。ホスト `/srv/minecraft-container-test` をコンテナ内 `/srv/minecraft-test` にidmap bind。
- ワールド: `/srv/minecraft-container-test/world`（復旧確認済み、2026-10-28時点約581MB）。
- Paper: 26.2。Javaの待受ポート: TCP 25566。コンテナIPは `/srv/minecraft-container-test/server.properties` で管理。
- Geyser-Spigot: 2.12.1-b1276、Floodgate: 2.2.5-SNAPSHOT、ViaVersion: 5.12.2、KokageWhitelist: 0.1.0（2026-10-28起動ログ）。
- コンテナのリソース上限: `mc-container-test.service.d/resources.conf`（MemoryMax=9G、CPUQuota=400%）。
- 旧ホスト側サービス: `minecraft.service`（2026-10-28停止確認）。旧データ: `/opt/minecraft/world`。稼働環境の更新対象は `/srv/minecraft-container-test`。

## 通信
- 中継: KAGOYA VPSのfrps + 自宅のfrpc（`frpc.service`、設定 `/etc/frp/frpc.toml`）。
- Java版: VPS TCP 25565 → コンテナ TCP 25566。
- 統合版NetherNet: VPS TCP/UDP 19132 → コンテナ TCP/UDP 19132。
- Geyser設定: `/srv/minecraft-container-test/plugins/Geyser-Spigot/config.yml`。NetherNetの`transport`、`signaling`、`advertise-addresses` はここで管理。
- VPSセキュリティグループ: Java TCP 25565、統合版 TCP/UDP 19132 を受信許可。SSH・frp制御ポートは接続元制限。
- 2026-10-28: TCP 19132のfrp中継と外部アクセスを確認。Geyser更新通知の消失とJava版でのワールド復旧を確認。

## バックアップ
- 定期実行: `mc-container-backup.timer` → `mc-container-backup.service` → `/usr/local/sbin/mc-container-backup.sh`。
- 保存先: `/var/backups/minecraft-container-test/`。2026-10-28 15:15のアーカイブを確認。
- 手動退避: `/opt/minecraft-recovery/world-container-current`（2026-10-28、約581MB）。
- プラグイン更新前のJARバックアップ: `/opt/minecraft-recovery/plugins-before-update/`。

## Geyser / Floodgate 更新
1. 接続中プレイヤーとワールドバックアップを確認。
2. `sudo systemctl stop mc-container-test.service`。
3. `/srv/minecraft-container-test/plugins/{Geyser-Spigot.jar,floodgate-spigot.jar}` を退避。
4. `https://download.geysermc.org/v2/projects/{geyser|floodgate}/versions/latest/builds/latest/downloads/spigot` から各JARを一時領域に取得、`unzip -t` で検証。
5. `sudo install -o minecraft -g minecraft -m 644` でコンテナ側 `plugins/` に配置。Paperとの互換性に応じてViaVersionも更新。
6. `sudo systemctl start mc-container-test.service`。
7. `sudo journalctl -u mc-container-test.service -n 150 --no-pager`、ゲーム内 `/geyser version`、統合版の実接続を確認。

## 管理アプリ / 開発状況
- Web: Nuxt、`kokage-manager.service`、`/opt/kokage-manager/app`、Tailscale内ポート3099。環境変数: `/etc/kokage-manager.env`。
- 2026-10-28: Webアプリ復旧。以前の起動失敗ログ: Tailscale待受IPの `EADDRNOTAVAIL`。
- 更新管理機能: [PR #1](https://github.com/ichiken26/minecraft-whitelist-app/pull/1)（更新ログ監視、ログイン時通知、Web手動更新、systemd起動の更新スクリプト）。2026-10-29時点 open / ready for review、サーバー適用・実機検証はこれから。
- 更新管理機能の設計対象: `mc-container-test.service`、キューディレクトリ `/srv/minecraft-container-test/plugins/KokageWhitelist`。
