package me.avie29.tabbylib.client.gui.widget;

import me.avie29.tabbylib.TabbyLibConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Category tab: plain text with an accent colored underline when selected. */
public class FlatTab extends AbstractButton {
	private final BooleanSupplier selected;
	private final Runnable onPress;

	public FlatTab(int width, int height, Component message, BooleanSupplier selected, Runnable onPress) {
		super(0, 0, width, height, message);
		this.selected = selected;
		this.onPress = onPress;
	}

	@Override
	public void onPress() {
		this.onPress.run();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		Font font = Minecraft.getInstance().font;
		boolean isSelected = this.selected.getAsBoolean();
		int x = this.getX();
		int y = this.getY();
		int accent = TabbyLibConfig.accentColor();

		if (isSelected) {
			graphics.fill(x, y, x + this.width, y + this.height, 0x40000000);
			graphics.fill(x, y + this.height - 2, x + this.width, y + this.height, accent);
		} else if (this.isHoveredOrFocused()) {
			graphics.fill(x, y, x + this.width, y + this.height, 0x30FFFFFF);
		}

		int color = isSelected ? 0xFFFFFFFF : this.isHoveredOrFocused() ? 0xFFE0E0E0 : 0xFFA0A0A0;
		Component text = this.getMessage();
		String clipped = font.plainSubstrByWidth(text.getString(), this.width - 4);
		Component shown = clipped.length() < text.getString().length() ? Component.literal(clipped) : text;
		graphics.drawCenteredString(font, shown, x + this.width / 2, y + (this.height - 8) / 2 - 1, color);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
