package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.avie29.tabbylib.TabbyLib;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the TabbyLib box texture ({@code textures/gui/config/menu.png}) at any size.
 * The texture is split into 9 parts: the corners keep their size, edges and center are stretched.
 */
public final class Panel {
	public static final ResourceLocation TEXTURE = TabbyLib.id("textures/gui/config/menu.png");
	private static final int TEXTURE_SIZE = 64;
	/** Size of the unstretched frame in the texture. Raise this if you draw a thicker frame. */
	private static final int BORDER = 4;

	private Panel() {
	}

	public static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
		int border = Math.min(BORDER, Math.min(width, height) / 2);
		int innerWidth = width - border * 2;
		int innerHeight = height - border * 2;
		int innerTexture = TEXTURE_SIZE - BORDER * 2;
		int right = x + width - border;
		int bottom = y + height - border;
		int textureEnd = TEXTURE_SIZE - BORDER;
		// The texture is half transparent, blending is off by default for textures in this version
		RenderSystem.enableBlend();

		// Corners
		part(graphics, x, y, border, border, 0, 0, border, border);
		part(graphics, right, y, border, border, textureEnd, 0, border, border);
		part(graphics, x, bottom, border, border, 0, textureEnd, border, border);
		part(graphics, right, bottom, border, border, textureEnd, textureEnd, border, border);

		// Edges
		part(graphics, x + border, y, innerWidth, border, BORDER, 0, innerTexture, border);
		part(graphics, x + border, bottom, innerWidth, border, BORDER, textureEnd, innerTexture, border);
		part(graphics, x, y + border, border, innerHeight, 0, BORDER, border, innerTexture);
		part(graphics, right, y + border, border, innerHeight, textureEnd, BORDER, border, innerTexture);

		// Center
		part(graphics, x + border, y + border, innerWidth, innerHeight, BORDER, BORDER, innerTexture, innerTexture);
		RenderSystem.disableBlend();
	}

	private static void part(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int srcWidth, int srcHeight) {
		if (width <= 0 || height <= 0) {
			return;
		}
		graphics.blit(TEXTURE, x, y, width, height, u, v, srcWidth, srcHeight, TEXTURE_SIZE, TEXTURE_SIZE);
	}
}
