package me.avie29.tabbylib.client.gui;

import me.avie29.tabbylib.client.gui.entry.OptionControls;
import net.minecraft.client.gui.screens.Screen;

/**
 * A screen that shows TabbyLib options with an {@link OptionListWidget}. The TabbyLib config screen is one,
 * mods with their own config screen implement it to reuse the TabbyLib option rows.
 * <p>
 * Key binding controls ask the host to capture the next key: forward {@code keyPressed} and
 * {@code mouseClicked} to the capturing control while {@link #isCapturing} is true, like
 * {@link TabbyConfigScreen} does.
 */
public interface OptionHost {
	/** The screen itself, used as parent for color picker, list editor and HUD editor. */
	Screen asScreen();

	/** Recreates the option rows, e.g. after a group was collapsed. */
	void rebuildOptions();

	/** Opens a sub screen (color picker, list editor, HUD editor) that returns to this screen. */
	void openSubScreen(Screen screen);

	/** A key binding control waits for the next key or mouse button. */
	void startKeyCapture(OptionControls.KeyBindControl control);

	boolean isCapturing(OptionControls.KeyBindControl control);
}
