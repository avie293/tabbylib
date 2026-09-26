package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import me.avie29.tabbylib.TabbyLib;
import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.compat.McCompat;
import me.avie29.tabbylib.platform.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Loads the icon from the mod metadata once per mod and keeps it as texture. */
public final class ModIcons {
	private static final Map<String, Optional<Icon>> CACHE = new HashMap<>();

	/** A loaded icon texture and its size in pixels. */
	public record Icon(ResourceLocation texture, int width, int height) {
		public void draw(GuiGraphics graphics, int x, int y, int size) {
			RenderSystem.enableBlend();
			graphics.blit(this.texture, x, y, size, size, 0, 0, this.width, this.height, this.width, this.height);
			RenderSystem.disableBlend();
		}
	}

	private ModIcons() {
	}

	public static @Nullable Icon get(TabbyConfig config) {
		if (config.getCustomIcon() != null) {
			// Custom icons are assumed to be square
			return new Icon(config.getCustomIcon(), 64, 64);
		}
		return CACHE.computeIfAbsent(config.getModId(), modId -> config.modInfo().flatMap(ModIcons::load)).orElse(null);
	}

	private static Optional<Icon> load(ModInfo mod) {
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
			ResourceLocation id = TabbyLib.id("mod_icon/" + safeId);
			Icon icon = new Icon(id, image.getWidth(), image.getHeight());
			Minecraft.getInstance().getTextureManager().register(id, McCompat.texture(image));
			return Optional.of(icon);
		} catch (Exception e) {
			TabbyLib.LOGGER.warn("Could not load icon of {}", mod.id(), e);
			return Optional.empty();
		}
	}
}
