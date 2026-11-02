package cc.avie.tabbylib.client.gui.control;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

public interface OptionControl {
	List<? extends AbstractWidget> widgets();

	void layout(int x, int y, int width, int height);

	void refresh();

	default void setActive(boolean active) {
		for (AbstractWidget widget : this.widgets()) {
			widget.active = active;
		}
	}
}
