package me.avie29.tabbylib.client;

import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.TabbyLibApi;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** The "Mod Settings..." button in the vanilla options screen. */
public final class OptionsButton {
	public static final int WIDTH = Button.DEFAULT_WIDTH * 2 + 8;

	private OptionsButton() {
	}

	public static boolean enabled() {
		return TabbyLibConfig.OPTIONS_BUTTON.get() && !TabbyLibApi.getConfigs().isEmpty();
	}

	public static Button create(Screen parent) {
		return Button.builder(Component.translatable("tabbylib.options_button"),
				button -> McCompat.setScreen(TabbyLibApi.createScreen(parent, null)))
			.width(WIDTH)
			.build();
	}

	/**
	 * For loaders that add the button with a screen event instead of the mixin: places it as a new row
	 * below the "Credits & Attribution" button, the last row of the option buttons.
	 */
	public static void addBelowGrid(Screen screen, List<? extends GuiEventListener> widgets, Consumer<Button> add) {
		if (!enabled()) {
			return;
		}
		AbstractWidget lastRow = find(widgets, "options.credits_and_attribution");
		AbstractWidget firstColumn = find(widgets, "options.skinCustomisation");
		if (lastRow == null || firstColumn == null) {
			return;
		}
		int gridTop = firstColumn.getY();
		int buttonY = lastRow.getY() + 24;

		// Not enough room above the "Done" button: move the option grid up into the gap below the header
		AbstractWidget done = find(widgets, "gui.done");
		if (done != null && buttonY + 24 > done.getY()) {
			int headerBottom = 0;
			for (GuiEventListener listener : widgets) {
				if (listener instanceof AbstractWidget widget && widget.getY() < gridTop) {
					headerBottom = Math.max(headerBottom, widget.getY() + widget.getHeight());
				}
			}
			int shift = Math.min(buttonY + 24 - done.getY(), Math.max(0, gridTop - headerBottom - 8));
			for (GuiEventListener listener : widgets) {
				if (listener instanceof AbstractWidget widget && widget.getY() >= gridTop && widget.getY() <= lastRow.getY()) {
					widget.setY(widget.getY() - shift);
				}
			}
			buttonY -= shift;
		}

		Button button = create(screen);
		McCompat.setBounds(button, firstColumn.getX(), buttonY, lastRow.getX() + lastRow.getWidth() - firstColumn.getX(), 20);
		add.accept(button);
	}

	private static @Nullable AbstractWidget find(List<? extends GuiEventListener> widgets, String translationKey) {
		for (GuiEventListener listener : widgets) {
			if (listener instanceof AbstractWidget widget
				&& widget.getMessage().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals(translationKey)) {
				return widget;
			}
		}
		return null;
	}
}
