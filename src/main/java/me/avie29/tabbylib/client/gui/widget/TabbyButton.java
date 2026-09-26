package me.avie29.tabbylib.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
	public void onPress() {
		this.onPress.accept(Screen.hasShiftDown());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == InputConstants.MOUSE_BUTTON_RIGHT && this.rightClick && this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.onPress.accept(true);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
