package cc.avie.tabbylib.client.gui.widget;

import cc.avie.tabbylib.TabbyLibConfig;
import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.client.gui.ModIcons;
import cc.avie.tabbylib.client.gui.TabbyGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class ModPanel extends AbstractButton {
	public static final int WIDTH = 88;
	public static final int HEIGHT = 82;
	private static final int LOGO_SIZE = 40;
	private static final int LOGO_TOP = 9;
	private static final int DIMMED = 0xFF5A5A5A;

	private final TabbyConfig config;
	private final Runnable onPress;
	/** The panel has a fixed width, so the texts are clipped once. */
	private final FormattedCharSequence name;
	private final @Nullable FormattedCharSequence version;
	private final String letter;

	public ModPanel(TabbyConfig config, Runnable onPress) {
		super(0, 0, WIDTH, HEIGHT, config.getDisplayName());
		this.config = config;
		this.onPress = onPress;
		Font font = Minecraft.getInstance().font;
		int textWidth = WIDTH - 8;
		this.name = TabbyGui.clip(font, this.getMessage(), textWidth);
		String version = TabbyGui.version(config.getVersion());
		this.version = version.isEmpty() ? null : TabbyGui.clip(font, Component.literal(version), textWidth);
		String name = this.getMessage().getString();
		this.letter = name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT);
		if (config.getDescription() != null) {
			this.setTooltip(Tooltip.create(config.getDescription()));
		}
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Font font = Minecraft.getInstance().font;
		boolean highlighted = this.isHoveredOrFocused();
		int x = this.getX();
		int y = this.getY();
		TabbyGui.panel(graphics, x, y, this.width, this.height);

		int logoX = x + (this.width - LOGO_SIZE) / 2;
		int logoY = y + LOGO_TOP;
		Identifier logo = ModIcons.get(this.config);
		if (logo != null) {
			TabbyGui.image(graphics, logo, logoX, logoY, LOGO_SIZE, highlighted ? DIMMED : -1);
		} else {
			int accent = TabbyLibConfig.accentColor();
			graphics.fill(logoX, logoY, logoX + LOGO_SIZE, logoY + LOGO_SIZE, TabbyGui.withAlpha(accent, highlighted ? 0x50 : 0xA0));
			if (!highlighted && !this.letter.isEmpty()) {
				graphics.centeredText(font, this.letter, logoX + LOGO_SIZE / 2, logoY + (LOGO_SIZE - 8) / 2, 0xFFFFFFFF);
			}
		}
		if (highlighted) {
			int size = TabbyGui.SETTINGS_OVERLAY_SIZE;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TabbyGui.SETTINGS_OVERLAY,
				logoX + (LOGO_SIZE - size) / 2, logoY + (LOGO_SIZE - size) / 2, size, size);
		}

		int centerX = x + this.width / 2;
		graphics.centeredText(font, this.name, centerX, logoY + LOGO_SIZE + 7, highlighted ? 0xFFFFFFFF : 0xFFE0E0E0);
		FormattedCharSequence version = this.version;
		if (version != null) {
			graphics.centeredText(font, version, centerX, logoY + LOGO_SIZE + 18, 0xFF808080);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
