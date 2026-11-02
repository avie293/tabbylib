package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.TabbyConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModIcons {
	private static final Map<String, Optional<Identifier>> CACHE = new HashMap<>();

	private ModIcons() {
	}

	public static @Nullable Identifier get(TabbyConfig config) {
		if (config.getCustomIcon() != null) {
			return config.getCustomIcon();
		}
		return CACHE.computeIfAbsent(config.getModId(), modId -> config.modContainer().flatMap(ModIcons::load)).orElse(null);
	}

	private static Optional<Identifier> load(ModContainer mod) {
		String id = mod.getMetadata().getId();
		Optional<Path> file = mod.getMetadata().getIconPath(128).flatMap(mod::findPath);
		if (file.isEmpty()) {
			return Optional.empty();
		}
		try {
			NativeImage image;
			try (InputStream input = Files.newInputStream(file.get())) {
				image = NativeImage.read(input);
			}
			String safeId = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
			Identifier texture = TabbyLib.id("mod_icon/" + safeId);
			Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "TabbyLib icon " + safeId, image));
			return Optional.of(texture);
		} catch (Exception e) {
			TabbyLib.LOGGER.warn("Could not load the icon of {}", id, e);
			return Optional.empty();
		}
	}
}
