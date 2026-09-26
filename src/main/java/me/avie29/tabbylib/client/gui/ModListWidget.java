package me.avie29.tabbylib.client.gui;

import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.TabbyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;

/** The vertical list of mods on the left side of the config screen. */
public class ModListWidget extends ObjectSelectionList<ModListWidget.ModEntry> {
	private static final int ENTRY_HEIGHT = 26;
	private final Consumer<TabbyConfig> onSelect;

	public ModListWidget(Minecraft minecraft, int x, int y, int width, int height, List<TabbyConfig> configs, Consumer<TabbyConfig> onSelect) {
		super(minecraft, width, height, y, ENTRY_HEIGHT);
		this.onSelect = onSelect;
		this.setX(x);
		this.centerListVertically = false;
		for (TabbyConfig config : configs) {
			this.addEntry(new ModEntry(config));
		}
	}

	public void select(TabbyConfig config) {
		for (ModEntry entry : this.children()) {
			if (entry.config == config) {
				this.setSelected(entry);
				return;
			}
		}
	}

	@Override
	public void setSelected(ModEntry entry) {
		ModEntry previous = this.getSelected();
		super.setSelected(entry);
		if (entry != null && entry != previous) {
			this.onSelect.accept(entry.config);
		}
	}

	@Override
	public int getRowWidth() {
		return this.width - 12;
	}

	@Override
	public int getRowLeft() {
		return this.getX() + 3;
	}

	@Override
	protected int scrollBarX() {
		return this.getRight() - 7;
	}

	@Override
	protected void extractListBackground(GuiGraphicsExtractor graphics) {
	}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor graphics) {
	}

	@Override
	protected void extractSelection(GuiGraphicsExtractor graphics, ModEntry entry, int outlineColor) {
		int accent = TabbyLibConfig.accentColor();
		int x0 = entry.getX();
		int y0 = entry.getY();
		int x1 = x0 + entry.getWidth();
		int y1 = y0 + entry.getHeight();
		graphics.fill(x0, y0, x1, y1, (accent & 0x00FFFFFF) | 0x33000000);
		graphics.fill(x0, y0, x0 + 2, y1, accent);
	}

	public class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
		private final TabbyConfig config;

		ModEntry(TabbyConfig config) {
			this.config = config;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = ModListWidget.this.minecraft.font;
			int x = this.getX() + 5;
			int y = this.getY();
			int right = this.getX() + this.getWidth() - 2;

			if (hovered && ModListWidget.this.getSelected() != this) {
				graphics.fill(this.getX(), y, this.getX() + this.getWidth(), y + this.getHeight(), 0x20FFFFFF);
			}

			if (TabbyLibConfig.MOD_ICONS.getPending()) {
				Identifier icon = ModIcons.get(this.config);
				if (icon != null) {
					graphics.blit(icon, x, y + 3, x + 20, y + 23, 0, 1, 0, 1);
				} else {
					// Placeholder: first letter on an accent colored square
					graphics.fill(x, y + 3, x + 20, y + 23, (TabbyLibConfig.accentColor() & 0x00FFFFFF) | 0x80000000);
					String letter = this.config.getDisplayName().getString().substring(0, 1).toUpperCase();
					graphics.centeredText(font, letter, x + 10, y + 9, 0xFFFFFFFF);
				}
				x += 24;
			}

			int textWidth = right - x - (this.config.hasChanges() ? 8 : 0);
			graphics.text(font, OptionListWidget.clip(font, this.config.getDisplayName(), textWidth), x, y + 4, 0xFFFFFFFF, true);
			graphics.text(font, OptionListWidget.clip(font, Component.literal(this.config.getVersion()), textWidth), x, y + 14, 0xFF808080, false);

			// Unsaved changes dot
			if (this.config.hasChanges()) {
				graphics.fill(right - 5, y + 5, right - 1, y + 9, TabbyLibConfig.accentColor());
			}
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			ModListWidget.this.setSelected(this);
			return true;
		}

		@Override
		public Component getNarration() {
			return Component.translatable("narrator.select", this.config.getDisplayName());
		}
	}
}
