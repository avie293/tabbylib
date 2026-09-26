package me.avie29.tabbylib.client.gui;

import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.compat.TabbySelectionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** The vertical list of mods on the left side of the config screen (legacy variant). */
public class ModListWidget extends TabbySelectionList<ModListWidget.ModEntry> {
	private static final int ENTRY_HEIGHT = 26;
	private final Consumer<TabbyConfig> onSelect;

	public ModListWidget(Minecraft minecraft, int x, int y, int width, int height, List<TabbyConfig> configs, Consumer<TabbyConfig> onSelect) {
		super(minecraft, x, y, width, height, ENTRY_HEIGHT);
		this.onSelect = onSelect;
		for (TabbyConfig config : configs) {
			this.add(new ModEntry(config));
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
	public void setSelected(@Nullable ModEntry entry) {
		ModEntry previous = this.getSelected();
		super.setSelected(entry);
		if (entry != null && entry != previous) {
			this.onSelect.accept(entry.config);
		}
	}

	@Override
	protected void renderSelection(GuiGraphics graphics, int top, int width, int height, int outerColor, int innerColor) {
		// Drawn by the entry itself in the accent color
	}

	public class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
		private final TabbyConfig config;

		ModEntry(TabbyConfig config) {
			this.config = config;
		}

		@Override
		public void render(GuiGraphics graphics, int index, int rowTop, int rowLeft, int rowWidth, int rowHeight,
						   int mouseX, int mouseY, boolean hovered, float delta) {
			Font font = ModListWidget.this.minecraft.font;
			int top = rowTop - 2;
			int bottom = top + ENTRY_HEIGHT;
			int x0 = rowLeft - 2;
			int right = rowLeft + rowWidth - 2;
			int accent = TabbyLibConfig.accentColor();

			if (ModListWidget.this.getSelected() == this) {
				graphics.fill(x0, top, right + 2, bottom, (accent & 0x00FFFFFF) | 0x33000000);
				graphics.fill(x0, top, x0 + 2, bottom, accent);
			} else if (hovered) {
				graphics.fill(x0, top, right + 2, bottom, 0x20FFFFFF);
			}

			int x = rowLeft + 3;
			if (TabbyLibConfig.MOD_ICONS.getPending()) {
				ModIcons.Icon icon = ModIcons.get(this.config);
				if (icon != null) {
					icon.draw(graphics, x, top + 3, 20);
				} else {
					// Placeholder: first letter on an accent colored square
					graphics.fill(x, top + 3, x + 20, top + 23, (accent & 0x00FFFFFF) | 0x80000000);
					String letter = this.config.getDisplayName().getString().substring(0, 1).toUpperCase();
					graphics.drawCenteredString(font, letter, x + 10, top + 9, 0xFFFFFFFF);
				}
				x += 24;
			}

			int textWidth = right - x - (this.config.hasChanges() ? 8 : 0);
			graphics.drawString(font, OptionListWidget.clip(font, this.config.getDisplayName(), textWidth), x, top + 4, 0xFFFFFFFF, true);
			graphics.drawString(font, OptionListWidget.clip(font, Component.literal(this.config.getVersion()), textWidth), x, top + 14, 0xFF808080, false);

			// Unsaved changes dot
			if (this.config.hasChanges()) {
				graphics.fill(right - 5, top + 5, right - 1, top + 9, accent);
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			ModListWidget.this.setSelected(this);
			return true;
		}

		@Override
		public Component getNarration() {
			return Component.translatable("narrator.select", this.config.getDisplayName());
		}
	}
}
