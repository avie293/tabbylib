package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.client.gui.widget.ModPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModGrid extends ContainerObjectSelectionList<ModGrid.Row> {
	public static final int GAP = 6;
	private final Consumer<TabbyConfig> onOpen;

	public ModGrid(Minecraft minecraft, int width, int height, int y, Consumer<TabbyConfig> onOpen) {
		super(minecraft, width, height, y, ModPanel.HEIGHT + GAP);
		this.onOpen = onOpen;
		this.centerListVertically = false;
	}

	public void setConfigs(List<TabbyConfig> configs) {
		this.clearEntries();
		int columns = this.columns();
		for (int start = 0; start < configs.size(); start += columns) {
			List<ModPanel> panels = new ArrayList<>();
			for (TabbyConfig config : configs.subList(start, Math.min(start + columns, configs.size()))) {
				panels.add(new ModPanel(config, () -> this.onOpen.accept(config)));
			}
			this.addEntry(new Row(panels));
		}
		this.setScrollAmount(0);
	}

	public boolean isEmpty() {
		return this.children().isEmpty();
	}

	private int columns() {
		return Math.max(1, (this.width - 16 + GAP) / (ModPanel.WIDTH + GAP));
	}

	@Override
	public int getRowWidth() {
		return this.columns() * (ModPanel.WIDTH + GAP) - GAP + 4;
	}

	@Override
	protected void extractListBackground(GuiGraphicsExtractor graphics) {
	}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor graphics) {
	}

	public static class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final List<ModPanel> panels;

		Row(List<ModPanel> panels) {
			this.panels = panels;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int x = this.getContentX();
			for (ModPanel panel : this.panels) {
				panel.setPosition(x, this.getY());
				panel.extractRenderState(graphics, mouseX, mouseY, a);
				x += ModPanel.WIDTH + GAP;
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.panels;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return this.panels;
		}
	}
}
