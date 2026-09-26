package me.avie29.tabbylib.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a HUD element inside the TabbyLib HUD editor. Usually you just call your normal HUD
 * render code with the given position.
 * <p>
 * The editor always uses the pending (unsaved) config values, so read your options with
 * {@code getPending()} here if you want the preview to react to unsaved changes.
 */
public interface HudPreview {
	/** Width of the element in GUI pixels (including scale). */
	int width();

	/** Height of the element in GUI pixels (including scale). */
	int height();

	/** Draws the element with its top left corner at x, y. */
	void render(GuiGraphics graphics, int x, int y);
}
