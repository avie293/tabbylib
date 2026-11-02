package cc.avie.tabbylib.neoforge;

import cc.avie.tabbylib.platform.Platform;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class NeoForgePlatform implements Platform {
	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public Optional<String> modName(String modId) {
		return mod(modId).map(IModInfo::getDisplayName);
	}

	@Override
	public Optional<String> modVersion(String modId) {
		return mod(modId).map(mod -> mod.getVersion().toString());
	}

	/** The square {@code iconFile} like in the NeoForge mod list, else the older {@code logoFile}. */
	@Override
	public @Nullable InputStream openModIcon(String modId) throws IOException {
		Optional<IModInfo> mod = mod(modId);
		if (mod.isEmpty()) {
			return null;
		}
		IModInfo info = mod.get();
		Optional<String> icon = info.getConfig().<String>getConfigElement("iconFile")
			.or(() -> info.getOwningFile().getConfig().getConfigElement("iconFile"))
			.or(info::getLogoFile);
		if (icon.isEmpty()) {
			return null;
		}
		JarContents contents = info.getOwningFile().getFile().getContents();
		return contents.containsFile(icon.get()) ? contents.openFile(icon.get()) : null;
	}

	private static Optional<IModInfo> mod(String modId) {
		return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo);
	}
}
