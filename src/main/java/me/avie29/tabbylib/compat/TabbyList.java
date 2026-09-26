package me.avie29.tabbylib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * Scrolling list of rows with widgets, without vanilla background and separators (1.21.1 variant).
 * All rows have the same height in this Minecraft version.
 */
public abstract class TabbyList<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList<E> {
	protected TabbyList(Minecraft minecraft, int x, int y, int width, int height, int rowHeight) {
		super(minecraft, width, height, y, rowHeight);
		this.setX(x);
	}

	public void setBounds(int x, int y, int width, int height) {
		this.updateSizeAndPosition(width, height, y);
		this.setX(x);
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
		return this.getX() + 4;
	}

	@Override
	protected int getScrollbarPosition() {
		return this.getRight() - 7;
	}

	@Override
	protected void renderListBackground(GuiGraphics graphics) {
	}

	@Override
	protected void renderListSeparators(GuiGraphics graphics) {
	}
}
