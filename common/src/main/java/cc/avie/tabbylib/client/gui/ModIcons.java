package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.platform.Platform;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
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
		return CACHE.computeIfAbsent(config.getModId(), ModIcons::load).orElse(null);
	}

	private static Optional<Identifier> load(String id) {
		try {
			NativeImage image;
			try (InputStream input = Platform.get().openModIcon(id)) {
				if (input == null) {
					return Optional.empty();
				}
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
