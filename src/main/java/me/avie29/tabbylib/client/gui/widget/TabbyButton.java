package me.avie29.tabbylib.client.gui.widget;

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
		this.onPress.accept(input.input() == 1 || input.hasShiftDown());
	}

	@Override
	protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
		return buttonInfo.button() == 0 || (this.rightClick && buttonInfo.button() == 1);
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
