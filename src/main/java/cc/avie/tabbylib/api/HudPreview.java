package cc.avie.tabbylib.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws a HUD element in the HUD editor. */
public interface HudPreview {
	/** Width of the element in GUI pixels (including scale). */
	int width();

	/** Height of the element in GUI pixels (including scale). */
	int height();

	/** Draws the element with its top left corner at x, y. */
	void render(GuiGraphicsExtractor graphics, int x, int y);
}
