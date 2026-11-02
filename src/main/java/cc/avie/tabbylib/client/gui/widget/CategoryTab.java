package cc.avie.tabbylib.client.gui.widget;

import cc.avie.tabbylib.TabbyLibConfig;
import cc.avie.tabbylib.client.gui.TabbyGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class CategoryTab extends AbstractButton {
	private final BooleanSupplier selected;
	private final Runnable onPress;

	public CategoryTab(int width, int height, Component message, BooleanSupplier selected, Runnable onPress) {
		super(0, 0, width, height, message);
		this.selected = selected;
		this.onPress = onPress;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Font font = Minecraft.getInstance().font;
		boolean isSelected = this.selected.getAsBoolean();
		boolean highlighted = this.isHoveredOrFocused();
		int x = this.getX();
		int y = this.getY();

		if (isSelected || highlighted) {
			TabbyGui.configBackground(graphics, x, y, this.width, this.height);
		}
		if (isSelected) {
			graphics.fill(x + 3, y + this.height - 2, x + this.width - 3, y + this.height - 1, TabbyLibConfig.accentColor());
		}

		int color = isSelected ? 0xFFFFFFFF : highlighted ? 0xFFE0E0E0 : 0xFFA0A0A0;
		graphics.centeredText(font, TabbyGui.clip(font, this.getMessage(), this.width - 6), x + this.width / 2, y + (this.height - 8) / 2, color);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
