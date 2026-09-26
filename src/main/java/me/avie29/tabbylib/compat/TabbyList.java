package me.avie29.tabbylib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * Scrolling list of rows with widgets, without vanilla background and separators (1.20.1 variant).
 * All rows have the same height in this Minecraft version.
 */
public abstract class TabbyList<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList<E> {
	protected TabbyList(Minecraft minecraft, int x, int y, int width, int height, int rowHeight) {
		super(minecraft, width, height, y, y + height, rowHeight);
		this.setLeftPos(x);
		this.setRenderBackground(false);
		this.setRenderTopAndBottom(false);
	}

	public void setBounds(int x, int y, int width, int height) {
		this.updateSize(width, height, y, y + height);
		this.setLeftPos(x);
	}

	public int rowHeight() {
		return this.itemHeight;
	}

	public void clear() {
		this.clearEntries();
	}

	public void add(E entry) {
		this.addEntry(entry);
	}

	public double scroll() {
		return this.getScrollAmount();
	}

	public void setScroll(double amount) {
		this.setScrollAmount(amount);
	}

	public int maxScroll() {
		return this.getMaxScroll();
	}

	@Override
	public int getRowWidth() {
		return this.width - 14;
	}

	@Override
	public int getRowLeft() {
		return this.x0 + 4;
	}

	@Override
	protected int getScrollbarPosition() {
		return this.x1 - 7;
	}
}
