package me.avie29.tabbylib.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

/**
 * The few Minecraft calls that differ between versions (1.21.1 variant). Each Minecraft version gets its own
 * copy of the compat package (see tools/ports.py), everything else stays identical. Internal, not part of the API.
 */
public final class McCompat {
	private McCompat() {
	}

	public static void setScreen(@Nullable Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	public static @Nullable Screen currentScreen() {
		return Minecraft.getInstance().screen;
	}

	public static ResourceLocation id(String namespace, String path) {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}

	public static DynamicTexture texture(NativeImage image) {
		return new DynamicTexture(image);
	}

	public static void setBounds(AbstractWidget widget, int x, int y, int width, int height) {
		widget.setRectangle(width, height, x, y);
	}

	/** Scrolls a text field back to the start of its text. */
	public static void resetCursor(EditBox box) {
		box.setCursorPosition(0);
		box.setHighlightPos(0);
	}
}
