package me.avie29.tabbylib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The few Minecraft calls that differ between versions. Each Minecraft version gets its own copy of this
 * class (see tools/ports.py), everything else stays identical. Internal, not part of the API.
 */
public final class McCompat {
	private McCompat() {
	}

	public static void setScreen(@Nullable Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}

	public static @Nullable Screen currentScreen() {
		return Minecraft.getInstance().gui.screen();
	}

	public static void setBounds(AbstractWidget widget, int x, int y, int width, int height) {
		widget.setRectangle(width, height, x, y);
	}

	/** Scrolls a text field back to the start of its text. */
	public static void resetCursor(EditBox box) {
		box.moveCursorToStart(false);
	}
}
