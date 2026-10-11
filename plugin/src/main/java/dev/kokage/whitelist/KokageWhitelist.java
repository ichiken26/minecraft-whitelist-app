package dev.kokage.whitelist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Never disable the whitelist; grant access only after authenticated UUID validation. */
public final class KokageWhitelist extends JavaPlugin implements Listener {
    private Path pending;
    private Path completed;

    @Override public void onEnable() {
        pending = getDataFolder().toPath().resolve("pending");
        completed = getDataFolder().toPath().resolve("completed");
        try { Files.createDirectories(pending); Files.createDirectories(completed); }
        catch (Exception e) { throw new IllegalStateException("Cannot prepare queue directories", e); }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Whitelist approval queue enabled. Keep enforce-whitelist=true.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        final UUID uuid = event.getUniqueId();
        final String actualName = event.getName();
        final boolean bedrock = isFloodgate(uuid);
        // Fail closed if Floodgate is absent or cannot verify the connecting identity.
        try (DirectoryStream<Path> files = Files.newDirectoryStream(pending, "*.json")) {
            for (Path path : files) {
                JsonObject request;
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    request = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (Exception ignored) { continue; }
                if (!request.has("name") || !request.has("edition") || !request.has("id") || !request.has("op")) continue;
                String requestedName = request.get("name").getAsString();
                String edition = request.get("edition").getAsString();
                if (!("bedrock".equals(edition) ? bedrock : ("java".equals(edition) && !bedrock))) continue;
                String normalized = bedrock ? actualName.replaceFirst("^[.]", "") : actualName;
                if (!normalized.toLowerCase(Locale.ROOT).equals(requestedName.toLowerCase(Locale.ROOT))) continue;
                String id = request.get("id").getAsString();
                if (!path.getFileName().toString().equals(id + ".json") || !id.matches("[0-9a-f-]{36}")) continue;
                boolean op = request.get("op").getAsBoolean();
                try {
                    // Bukkit whitelist/operator mutations must execute on the primary thread.
                    Boolean saved = Bukkit.getScheduler().callSyncMethod(this, () -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        player.setWhitelisted(true);
                        if (op) player.setOp(true);
                        return player.isWhitelisted() && (!op || player.isOp());
                    }).get(5, TimeUnit.SECONDS);
                    if (!Boolean.TRUE.equals(saved)) return;
                    request.addProperty("uuid", uuid.toString());
                    request.addProperty("registeredName", actualName);
                    request.addProperty("completedAt", java.time.Instant.now().toString());
                    // Write a new file, then atomically move it into completed; only then remove queue item.
                    Path temporary = Files.createTempFile(completed, ".approval-", ".tmp");
                    try {
                        Files.writeString(temporary, request.toString(), StandardCharsets.UTF_8);
                        Files.move(temporary, completed.resolve(id + ".json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(path);
                    } finally { Files.deleteIfExists(temporary); }
                    getLogger().info("Approved " + actualName + " (" + edition + ", " + uuid + ")");
                    // If the whitelist was checked earlier in the pipeline, player can reconnect.
                    return;
                } catch (Exception e) {
                    getLogger().warning("Approval failed for " + actualName + ": " + e.getMessage());
                    return;
                }
            }
        } catch (Exception e) { getLogger().warning("Cannot read approval queue: " + e.getMessage()); }
    }

    private boolean isFloodgate(UUID uuid) {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(api.getMethod("isFloodgatePlayer", UUID.class).invoke(instance, uuid));
        } catch (Exception | LinkageError e) {
            getLogger().warning("Floodgate identity unavailable: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
