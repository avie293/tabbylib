package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.TabbyLib;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class TabbyGui {
	public static final Identifier PANEL = TabbyLib.id("config/mod_panel");
	public static final Identifier CONFIG_BACKGROUND = TabbyLib.id("config/config_background");
	public static final Identifier SETTINGS_OVERLAY = TabbyLib.id("config/settings_overlay");
	public static final int SETTINGS_OVERLAY_SIZE = 20;

	private TabbyGui() {
	}

	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, width, height);
	}

	public static void configBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CONFIG_BACKGROUND, x, y, width, height);
	}

	public static String version(String version) {
		if (version.isEmpty() || version.startsWith("v") || version.startsWith("V")) {
			return version;
		}
		return "v" + version;
	}

	public static void image(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size, int tint) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, 1, 1, 1, 1, tint);
	}

	public static FormattedCharSequence clip(Font font, Component text, int width) {
		if (font.width(text) <= width) {
			return text.getVisualOrderText();
		}
		String ellipsis = "...";
		String clipped = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width(ellipsis)));
		return Component.literal(clipped + ellipsis).withStyle(text.getStyle()).getVisualOrderText();
	}

	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | alpha << 24;
	}
}
