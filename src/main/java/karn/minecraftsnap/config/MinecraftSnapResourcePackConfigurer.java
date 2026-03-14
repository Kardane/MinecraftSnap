package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinecraftSnapResourcePackConfigurer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final MinecraftServer server;
	private final Logger logger;

	public MinecraftSnapResourcePackConfigurer(MinecraftServer server, Logger logger) {
		this.server = server;
		this.logger = logger;
	}

	public void applyDefaults() {
		Path resourcePackConfig = server.getRunDirectory()
			.resolve("config")
			.resolve("polymer")
			.resolve("resource-pack.json");
		Path autoHostConfig = server.getRunDirectory()
			.resolve("config")
			.resolve("polymer")
			.resolve("auto-host.json");

		try {
			if (Files.notExists(resourcePackConfig)) {
				return;
			}

			JsonObject json = JsonParser.parseString(Files.readString(resourcePackConfig)).getAsJsonObject();
			JsonArray includeModAssets = new JsonArray();
			if (json.has("include_mod_assets") && json.get("include_mod_assets").isJsonArray()) {
				for (var element : json.getAsJsonArray("include_mod_assets")) {
					if (element.isJsonPrimitive()) {
						String modId = element.getAsString();
						if (!MinecraftSnap.MOD_ID.equals(modId)) {
							includeModAssets.add(modId);
						}
					}
				}
			}
			includeModAssets.add(MinecraftSnap.MOD_ID);
			json.add("include_mod_assets", includeModAssets);
			json.addProperty("markResourcePackAsRequiredByDefault", true);
			Files.writeString(resourcePackConfig, GSON.toJson(json));

			if (Files.exists(autoHostConfig)) {
				JsonObject autoHost = JsonParser.parseString(Files.readString(autoHostConfig)).getAsJsonObject();
				autoHost.addProperty("enabled", true);
				autoHost.addProperty("required", true);
				autoHost.addProperty("setup_early", true);
				Files.writeString(autoHostConfig, GSON.toJson(autoHost));
			}
		} catch (IOException e) {
			logger.warn("[{}] 리소스팩 자동 제공 설정 반영 실패", MinecraftSnap.MOD_ID, e);
		}
	}
}
