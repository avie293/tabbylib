package cc.avie.tabbylib.fabric;

import cc.avie.tabbylib.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FabricPlatform implements Platform {
	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public Optional<String> modName(String modId) {
		return mod(modId).map(mod -> mod.getMetadata().getName());
	}

	@Override
	public Optional<String> modVersion(String modId) {
		return mod(modId).map(mod -> mod.getMetadata().getVersion().getFriendlyString());
	}

	@Override
	public @Nullable InputStream openModIcon(String modId) throws IOException {
		Optional<Path> file = mod(modId).flatMap(mod -> mod.getMetadata().getIconPath(128).flatMap(mod::findPath));
		return file.isPresent() ? Files.newInputStream(file.get()) : null;
	}

	private static Optional<ModContainer> mod(String modId) {
		return FabricLoader.getInstance().getModContainer(modId);
	}
}
