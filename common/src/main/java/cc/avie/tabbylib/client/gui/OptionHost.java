package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.client.gui.control.OptionControls;
import net.minecraft.client.gui.screens.Screen;

public interface OptionHost {
	Screen asScreen();

	void rebuildOptions();

	void openSubScreen(Screen screen);

	void startKeyCapture(OptionControls.KeyBindControl control);

	boolean isCapturing(OptionControls.KeyBindControl control);
}
