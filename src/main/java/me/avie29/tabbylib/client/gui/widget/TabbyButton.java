package me.avie29.tabbylib.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Vanilla styled button that also reacts to right clicks.
 * The handler receives true for the "secondary" action: right click or Shift + click.
 */
public class TabbyButton extends AbstractButton {
	private final Consumer<Boolean> onPress;
	private final boolean rightClick;

	public TabbyButton(int width, int height, Component message, boolean rightClick, Consumer<Boolean> onPress) {
		super(0, 0, width, height, message);
		this.onPress = onPress;
		this.rightClick = rightClick;
	}

	public TabbyButton(int width, int height, Component message, Runnable onPress) {
		this(width, height, message, false, secondary -> onPress.run());
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.accept(input.input() == InputConstants.MOUSE_BUTTON_RIGHT || input.hasShiftDown());
	}

	@Override
	protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
		return buttonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT
			|| (this.rightClick && buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractDefaultSprite(graphics);
		this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
