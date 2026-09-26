package me.avie29.tabbylib.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/** A clickable colored square. Transparent colors are drawn over a checkerboard. */
public class ColorSwatch extends AbstractButton {
	private final IntSupplier color;
	private final Runnable onPress;

	public ColorSwatch(int size, IntSupplier color, Runnable onPress) {
		super(0, 0, size, size, Component.translatable("tabbylib.color.pick"));
		this.color = color;
		this.onPress = onPress;
	}

	@Override
	public void onPress() {
		this.onPress.run();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int x = this.getX();
		int y = this.getY();
		int outline = !this.active ? 0xFF555555 : this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
		graphics.fill(x, y, x + this.width, y + this.height, outline);
		drawColor(graphics, x + 1, y + 1, x + this.width - 1, y + this.height - 1, this.color.getAsInt());
	}

	/** Fills a rectangle with the color over a checkerboard, so transparency is visible. */
	public static void drawColor(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
		checkerboard(graphics, x0, y0, x1, y1);
		graphics.fill(x0, y0, x1, y1, argb);
	}

	public static void checkerboard(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
		int cell = 4;
		graphics.fill(x0, y0, x1, y1, 0xFFFFFFFF);
		for (int cy = y0; cy < y1; cy += cell) {
			for (int cx = x0 + (((cy - y0) / cell) % 2) * cell; cx < x1; cx += cell * 2) {
				graphics.fill(cx, cy, Math.min(cx + cell, x1), Math.min(cy + cell, y1), 0xFFBFBFBF);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
