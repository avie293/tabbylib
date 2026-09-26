package me.avie29.tabbylib.client.gui.entry;

import com.mojang.blaze3d.platform.InputConstants;
import me.avie29.tabbylib.api.option.BooleanOption;
import me.avie29.tabbylib.api.option.ColorOption;
import me.avie29.tabbylib.api.option.DoubleOption;
import me.avie29.tabbylib.api.option.EnumOption;
import me.avie29.tabbylib.api.option.HudPositionOption;
import me.avie29.tabbylib.api.option.IntOption;
import me.avie29.tabbylib.api.option.KeyBindOption;
import me.avie29.tabbylib.api.option.Option;
import me.avie29.tabbylib.api.option.StringListOption;
import me.avie29.tabbylib.api.option.StringOption;
import me.avie29.tabbylib.client.gui.ColorPickerScreen;
import me.avie29.tabbylib.client.gui.HudEditorScreen;
import me.avie29.tabbylib.client.gui.StringListScreen;
import me.avie29.tabbylib.client.gui.TabbyConfigScreen;
import me.avie29.tabbylib.client.gui.widget.ColorSwatch;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import me.avie29.tabbylib.client.gui.widget.ValueSlider;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/** Creates the fitting control for each option type. */
public final class OptionControls {
	private static final int TEXT_COLOR = EditBox.DEFAULT_TEXT_COLOR;
	private static final int ERROR_COLOR = 0xFFFF5555;

	private OptionControls() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static OptionControl create(Option<?> option, TabbyConfigScreen screen) {
		Font font = Minecraft.getInstance().font;
		if (option instanceof BooleanOption bool) {
			return new BooleanControl(bool);
		}
		if (option instanceof IntOption number) {
			return number.isSlider()
				? new SliderControl(number.getMin(), number.getMax(), number.getStep(),
				() -> number.getPending(), value -> number.setPending((int) Math.round(value)), value -> number.valueText((int) Math.round(value)))
				: new TextControl<>(font, number, value -> Integer.toString(value), text -> {
				int parsed = Integer.parseInt(text.trim());
				return parsed >= number.getMin() && parsed <= number.getMax() ? parsed : null;
			});
		}
		if (option instanceof DoubleOption number) {
			return number.isSlider()
				? new SliderControl(number.getMin(), number.getMax(), number.getStep(),
				number::getPending, number::setPending, number::valueText)
				: new TextControl<>(font, number, number::formatValue, text -> {
				double parsed = Double.parseDouble(text.trim().replace(',', '.'));
				return parsed >= number.getMin() && parsed <= number.getMax() ? parsed : null;
			});
		}
		if (option instanceof StringOption string) {
			TextControl<String> control = new TextControl<>(font, string, Function.identity(), text -> string.isValid(text) ? text : null);
			control.box.setMaxLength(string.getMaxLength());
			if (string.getHint() != null) {
				control.box.setHint(string.getHint());
			}
			return control;
		}
		if (option instanceof EnumOption enumOption) {
			return new EnumControl<>(enumOption);
		}
		if (option instanceof ColorOption color) {
			return new ColorControl(font, color, screen);
		}
		if (option instanceof KeyBindOption keyBind) {
			return new KeyBindControl(keyBind, screen);
		}
		if (option instanceof StringListOption list) {
			return new ButtonControl(
				() -> Component.translatable("tabbylib.list.edit", list.getPending().size()),
				() -> screen.openSubScreen(new StringListScreen(screen, list)));
		}
		if (option instanceof HudPositionOption hud) {
			return new ButtonControl(
				() -> Component.translatable("tabbylib.hud.edit"),
				() -> screen.openSubScreen(new HudEditorScreen(screen, hud)));
		}
		// Unknown option type from another mod: show the value read only
		ButtonControl fallback = new ButtonControl(() -> Component.literal(describe(option)), () -> {
		});
		fallback.button.active = false;
		return fallback;
	}

	private static <T> String describe(Option<T> option) {
		return option.formatValue(option.getPending());
	}

	// ---------------------------------------------------------------- boolean

	private static final class BooleanControl implements OptionControl {
		private final BooleanOption option;
		private final TabbyButton button;

		BooleanControl(BooleanOption option) {
			this.option = option;
			this.button = new TabbyButton(0, 20, Component.empty(), () -> {
				option.setPending(!option.getPending());
				this.refresh();
			});
			this.refresh();
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.button);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			boolean value = this.option.getPending();
			MutableComponent text = this.option.valueText(value).copy();
			this.button.setMessage(text.withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}

	// ---------------------------------------------------------------- slider

	private static final class SliderControl implements OptionControl {
		private final java.util.function.DoubleSupplier getter;
		private final ValueSlider slider;

		SliderControl(double min, double max, double step, java.util.function.DoubleSupplier getter,
					  java.util.function.DoubleConsumer setter, java.util.function.DoubleFunction<Component> formatter) {
			this.getter = getter;
			this.slider = new ValueSlider(0, 20, min, max, step, getter.getAsDouble(), formatter, setter);
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.slider);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.slider, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.slider.setActualValue(this.getter.getAsDouble());
		}
	}

	// ---------------------------------------------------------------- text / numbers

	private static final class TextControl<T> implements OptionControl {
		private final Option<T> option;
		private final Function<T, String> toText;
		final EditBox box;
		private boolean updating;

		/**
		 * @param parser returns null (or throws) for invalid input
		 */
		TextControl(Font font, Option<T> option, Function<T, String> toText, Function<String, @Nullable T> parser) {
			this.option = option;
			this.toText = toText;
			this.box = new EditBox(font, 0, 0, 150, 20, option.getName());
			this.box.setMaxLength(1024);
			this.box.setValue(toText.apply(option.getPending()));
			this.box.setResponder(text -> {
				if (this.updating) {
					return;
				}
				T parsed;
				try {
					parsed = parser.apply(text);
				} catch (RuntimeException e) {
					parsed = null;
				}
				if (parsed != null) {
					option.setPending(parsed);
					this.box.setTextColor(TEXT_COLOR);
				} else {
					this.box.setTextColor(ERROR_COLOR);
				}
			});
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.box);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			boolean resized = this.box.getWidth() != width - 2;
			McCompat.setBounds(this.box, x + 1, y + 1, width - 2, height - 2);
			if (resized && !this.box.isFocused()) {
				// Shows the start of the text again after the width changed
				McCompat.resetCursor(this.box);
			}
		}

		@Override
		public void refresh() {
			String text = this.toText.apply(this.option.getPending());
			if (!text.equals(this.box.getValue())) {
				this.updating = true;
				this.box.setValue(text);
				this.updating = false;
			}
			this.box.setTextColor(TEXT_COLOR);
		}
	}

	// ---------------------------------------------------------------- enum

	private static final class EnumControl<E extends Enum<E>> implements OptionControl {
		private final EnumOption<E> option;
		private final TabbyButton button;

		EnumControl(EnumOption<E> option) {
			this.option = option;
			this.button = new TabbyButton(0, 20, Component.empty(), true, secondary -> {
				int direction = secondary ? -1 : 1;
				option.setPending(option.cycle(option.getPending(), direction));
				this.refresh();
			});
			this.refresh();
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.button);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.button.setMessage(this.option.valueText(this.option.getPending()));
		}
	}

	// ---------------------------------------------------------------- color

	private static final class ColorControl implements OptionControl {
		private final ColorOption option;
		private final ColorSwatch swatch;
		private final TextControl<Integer> text;

		ColorControl(Font font, ColorOption option, TabbyConfigScreen screen) {
			this.option = option;
			this.text = new TextControl<>(font, option, value -> ColorOption.toHex(value, option.hasAlpha()), input -> {
				Integer parsed = ColorOption.parseHex(input);
				if (parsed == null) {
					return null;
				}
				// "#RRGGBB" typed into an alpha field keeps full opacity, alpha is ignored without alpha support
				return option.hasAlpha() ? parsed : parsed | 0xFF000000;
			});
			this.swatch = new ColorSwatch(20, option::getPending, () -> screen.openSubScreen(
				new ColorPickerScreen(screen, option.getName(), option.getPending(), option.hasAlpha(), picked -> {
					option.setPending(picked);
					this.refresh();
				})));
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.swatch, this.text.box);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.swatch, x, y, height, height);
			this.text.layout(x + height + 4, y, width - height - 4, height);
		}

		@Override
		public void refresh() {
			this.text.refresh();
		}
	}

	// ---------------------------------------------------------------- key binds

	public static final class KeyBindControl implements OptionControl {
		private final KeyBindOption option;
		private final TabbyButton button;
		private final TabbyConfigScreen screen;

		KeyBindControl(KeyBindOption option, TabbyConfigScreen screen) {
			this.option = option;
			this.screen = screen;
			this.button = new TabbyButton(0, 20, Component.empty(), () -> {
				screen.startKeyCapture(this);
				this.refresh();
			});
			this.refresh();
		}

		public void setKey(InputConstants.Key key) {
			this.option.setPending(key);
			this.refresh();
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.button);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.button, x, y, width, height);
			this.refresh();
		}

		@Override
		public void refresh() {
			InputConstants.Key key = this.option.getPending();
			MutableComponent text = key.getDisplayName().copy();
			if (this.screen.isCapturing(this)) {
				text = Component.literal("> ").append(text.withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <").withStyle(ChatFormatting.YELLOW);
			}
			List<Component> conflicts = this.conflicts(key);
			if (!conflicts.isEmpty() && !this.screen.isCapturing(this)) {
				text = Component.literal("[ ").append(text).append(" ]").withStyle(ChatFormatting.RED);
				this.button.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds",
					ComponentUtils.formatList(conflicts, Component.literal(", ")))));
			} else {
				this.button.setTooltip(null);
			}
			this.button.setMessage(text);
		}

		/** Other key mappings on the same key. Like vanilla, two mappings that are both on their default key are fine. */
		private List<Component> conflicts(InputConstants.Key key) {
			if (key.equals(InputConstants.UNKNOWN)) {
				return List.of();
			}
			KeyMapping own = this.option.getMapping();
			boolean ownDefault = key.getName().equals(own.getDefaultKey().getName());
			String name = key.getName();
			List<Component> result = new java.util.ArrayList<>();
			for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
				if (other != own && other.saveString().equals(name) && !(ownDefault && other.isDefault())) {
					result.add(Component.translatable(other.getName()));
				}
			}
			return result;
		}
	}

	// ---------------------------------------------------------------- button that opens something

	private static final class ButtonControl implements OptionControl {
		private final java.util.function.Supplier<Component> text;
		private final TabbyButton button;

		ButtonControl(java.util.function.Supplier<Component> text, Runnable onPress) {
			this.text = text;
			this.button = new TabbyButton(0, 20, text.get(), onPress);
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.button);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			McCompat.setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.button.setMessage(this.text.get());
		}
	}
}
