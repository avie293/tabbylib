package me.avie29.tabbylib.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for TabbyLib screens that hides how the background is drawn in this Minecraft version (1.21.1).
 * Order: {@link #drawBackground} - {@link #renderContent} - widgets - {@link #renderForeground}.
 */
public abstract class TabbyScreen extends Screen {
	protected TabbyScreen(Component title) {
		super(title);
	}

	/** Blurred menu background by default. */
	protected void drawBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(graphics, mouseX, mouseY, delta);
	}

	/** Drawn above the background but below the widgets (panels, labels). */
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	}

	/** Drawn above the widgets. */
	protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.drawBackground(graphics, mouseX, mouseY, delta);
		this.renderContent(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		this.renderForeground(graphics, mouseX, mouseY, delta);
	}
}
