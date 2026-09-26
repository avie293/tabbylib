package me.avie29.tabbylib.client.gui.entry;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

/** The widgets that edit one option, placed on the right side of an option row. */
public interface OptionControl {
	List<? extends AbstractWidget> widgets();

	/** Positions the widgets inside the given area. */
	void layout(int x, int y, int width, int height);

	/** Updates the widgets from the pending value, e.g. after "reset". */
	void refresh();

	default void setActive(boolean active) {
		for (AbstractWidget widget : this.widgets()) {
			widget.active = active;
		}
	}
}
