package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.api.TabbyLibApi;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.Optional;

/** NeoForge implementation of the few loader specific things TabbyLib needs. */
public final class Platform {
	private Platform() {
	}

	public static Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static Optional<ModInfo> mod(String modId) {
		return ModList.get().getModContainerById(modId).map(container -> toInfo(container.getModInfo()));
	}

	/** Makes the "Config" button in NeoForge's mod list open the TabbyLib screen. */
	public static void onConfigRegistered(TabbyConfig config) {
		ModList.get().getModContainerById(config.getModId()).ifPresent(container ->
			container.registerExtensionPoint(IConfigScreenFactory.class,
				(IConfigScreenFactory) (modContainer, parent) -> TabbyLibApi.createScreen(parent, config.getModId())));
	}

	private static ModInfo toInfo(IModInfo info) {
		return new ModInfo(info.getModId(), info.getDisplayName(), info.getVersion().toString(), info.getLogoFile(), path -> {
			var contents = info.getOwningFile().getFile().getContents();
			return contents.findFile(path).isPresent() ? Optional.of(contents.openFile(path)) : Optional.empty();
		});
	}
}
