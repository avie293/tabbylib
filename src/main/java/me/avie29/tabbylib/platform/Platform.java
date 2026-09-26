package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.api.TabbyLibApi;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Forge implementation of the few loader specific things TabbyLib needs. */
public final class Platform {
	private Platform() {
	}

	public static Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static Optional<ModInfo> mod(String modId) {
		return ModList.get().getModContainerById(modId).map(container -> toInfo(container.getModInfo()));
	}

	/** Makes the "Config" button in Forge's mod list open the TabbyLib screen. */
	public static void onConfigRegistered(TabbyConfig config) {
		ModList.get().getModContainerById(config.getModId()).ifPresent(container ->
			container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> TabbyLibApi.createScreen(parent, config.getModId()))));
	}

	private static ModInfo toInfo(IModInfo info) {
		return new ModInfo(info.getModId(), info.getDisplayName(), info.getVersion().toString(), info.getLogoFile(), path -> {
			Path file = info.getOwningFile().getFile().findResource(path);
			return Files.exists(file) ? Optional.of(Files.newInputStream(file)) : Optional.empty();
		});
	}
}
