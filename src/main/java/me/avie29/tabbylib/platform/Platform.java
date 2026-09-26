package me.avie29.tabbylib.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Fabric implementation of the few loader specific things TabbyLib needs. Every loader has its own version of this class. */
public final class Platform {
	private Platform() {
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static Optional<ModInfo> mod(String modId) {
		return FabricLoader.getInstance().getModContainer(modId).map(Platform::toInfo);
	}

	/** Fabric has no config button in a mod list of its own (Mod Menu is optional), so nothing to do. */
	public static void onConfigRegistered(me.avie29.tabbylib.api.TabbyConfig config) {
	}

	private static ModInfo toInfo(ModContainer mod) {
		var metadata = mod.getMetadata();
		return new ModInfo(metadata.getId(), metadata.getName(), metadata.getVersion().getFriendlyString(),
			metadata.getIconPath(64), path -> {
			Optional<Path> file = mod.findPath(path);
			return file.isPresent() ? Optional.of(Files.newInputStream(file.get())) : Optional.empty();
		});
	}
}
