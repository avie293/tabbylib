package me.avie29.tabbylib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

/** Selectable list without vanilla background and separators (1.20.1 variant). */
public abstract class TabbySelectionList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
	protected TabbySelectionList(Minecraft minecraft, int x, int y, int width, int height, int rowHeight) {
		super(minecraft, width, height, y, y + height, rowHeight);
		this.setLeftPos(x);
		this.setRenderBackground(false);
		this.setRenderTopAndBottom(false);
	}

	public void add(E entry) {
		this.addEntry(entry);
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
