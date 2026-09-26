package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import me.avie29.tabbylib.TabbyLib;
import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.platform.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Loads the icon from the mod metadata once per mod and keeps it as texture. */
public final class ModIcons {
	private static final Map<String, Optional<Identifier>> CACHE = new HashMap<>();

	private ModIcons() {
	}

	public static @Nullable Identifier get(TabbyConfig config) {
		if (config.getCustomIcon() != null) {
			return config.getCustomIcon();
		}
		return CACHE.computeIfAbsent(config.getModId(), modId -> config.modInfo().flatMap(ModIcons::load)).orElse(null);
	}

	private static Optional<Identifier> load(ModInfo mod) {
		if (mod.iconPath().isEmpty()) {
			return Optional.empty();
		}
		try {
			Optional<InputStream> stream = mod.resources().open(mod.iconPath().get());
			if (stream.isEmpty()) {
				return Optional.empty();
			}
			NativeImage image;
			try (InputStream input = stream.get()) {
				image = NativeImage.read(input);
			}
			String safeId = mod.id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
			Identifier id = TabbyLib.id("mod_icon/" + safeId);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "TabbyLib icon " + safeId, image));
			return Optional.of(id);
		} catch (Exception e) {
			TabbyLib.LOGGER.warn("Could not load icon of {}", mod.id(), e);
			return Optional.empty();
		}
	}
}
