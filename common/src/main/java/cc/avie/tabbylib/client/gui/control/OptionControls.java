package cc.avie.tabbylib.client.gui.control;

import cc.avie.tabbylib.api.option.BooleanOption;
import cc.avie.tabbylib.api.option.ColorOption;
import cc.avie.tabbylib.api.option.DoubleOption;
import cc.avie.tabbylib.api.option.EnumOption;
import cc.avie.tabbylib.api.option.HudPositionOption;
import cc.avie.tabbylib.api.option.IntOption;
import cc.avie.tabbylib.api.option.KeyBindOption;
import cc.avie.tabbylib.api.option.Option;
import cc.avie.tabbylib.api.option.StringListOption;
import cc.avie.tabbylib.api.option.StringOption;
import cc.avie.tabbylib.client.gui.ColorPickerScreen;
import cc.avie.tabbylib.client.gui.HudEditorScreen;
import cc.avie.tabbylib.client.gui.OptionHost;
import cc.avie.tabbylib.client.gui.StringListScreen;
import cc.avie.tabbylib.client.gui.widget.ColorSwatch;
import cc.avie.tabbylib.client.gui.widget.TabbyButton;
import cc.avie.tabbylib.client.gui.widget.ValueSlider;
import com.mojang.blaze3d.platform.InputConstants;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OptionControls {
	private static final int TEXT_COLOR = EditBox.DEFAULT_TEXT_COLOR;
	private static final int ERROR_COLOR = 0xFFFF5555;

	private OptionControls() {
	}

	public static OptionControl create(Option<?, ?> option, OptionHost host) {
		Font font = Minecraft.getInstance().font;
		return switch (option) {
			case BooleanOption toggle -> new BooleanControl(toggle);
			case IntOption number -> number.isSlider()
				? new SliderControl(number.getMin(), number.getMax(), number.getStep(), number::getPending,
				value -> number.setPending((int) Math.round(value)), value -> number.valueText((int) Math.round(value)))
				: new TextControl<>(font, number, value -> Integer.toString(value), text -> {
				int parsed = Integer.parseInt(text.trim());
				return parsed >= number.getMin() && parsed <= number.getMax() ? parsed : null;
			});
			case DoubleOption number -> number.isSlider()
				? new SliderControl(number.getMin(), number.getMax(), number.getStep(), number::getPending,
				number::setPending, number::valueText)
				: new TextControl<>(font, number, number::formatValue, text -> {
				double parsed = Double.parseDouble(text.trim().replace(',', '.'));
				return parsed >= number.getMin() && parsed <= number.getMax() ? parsed : null;
			});
			case StringOption text -> {
				TextControl<String> control = new TextControl<>(font, text, Function.identity(), input -> text.isValid(input) ? input : null);
				control.box.setMaxLength(text.getMaxLength());
				if (text.getHint() != null) {
					control.box.setHint(text.getHint());
				}
				yield control;
			}
			case EnumOption<?> choice -> new EnumControl<>(choice);
			case ColorOption color -> new ColorControl(font, color, host);
			case KeyBindOption keyBind -> new KeyBindControl(keyBind, host);
			case StringListOption list -> new ButtonControl(
				() -> Component.translatable("tabbylib.list.edit", list.getPending().size()),
				() -> host.openSubScreen(new StringListScreen(host.asScreen(), list)));
			case HudPositionOption hud -> new ButtonControl(
				() -> Component.translatable("tabbylib.hud.edit"),
				() -> host.openSubScreen(new HudEditorScreen(host.asScreen(), hud)));
			default -> {
				ButtonControl fallback = new ButtonControl(() -> Component.literal(describe(option)), () -> {
				});
				fallback.button.active = false;
				yield fallback;
			}
		};
	}

	private static <T> String describe(Option<T, ?> option) {
		return option.formatValue(option.getPending());
	}

	private static void setBounds(AbstractWidget widget, int x, int y, int width, int height) {
		widget.setRectangle(width, height, x, y);
	}

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
			setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			boolean value = this.option.getPending();
			this.button.setMessage(this.option.valueText(value).copy().withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}

	private static final class SliderControl implements OptionControl {
		private final DoubleSupplier getter;
		private final ValueSlider slider;

		SliderControl(double min, double max, double step, DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<Component> formatter) {
			this.getter = getter;
			this.slider = new ValueSlider(min, max, step, getter.getAsDouble(), formatter, setter);
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.slider);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			setBounds(this.slider, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.slider.setActualValue(this.getter.getAsDouble());
		}
	}

	private static final class TextControl<T> implements OptionControl {
		private final Option<T, ?> option;
		private final Function<T, String> toText;
		final EditBox box;
		private boolean updating;

		TextControl(Font font, Option<T, ?> option, Function<T, String> toText, Function<String, @Nullable T> parser) {
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
			setBounds(this.box, x + 1, y + 1, width - 2, height - 2);
			if (resized && !this.box.isFocused()) {
				this.box.moveCursorToStart(false);
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

	private static final class EnumControl<E extends Enum<E>> implements OptionControl {
		private final EnumOption<E> option;
		private final TabbyButton button;

		EnumControl(EnumOption<E> option) {
			this.option = option;
			this.button = new TabbyButton(0, 20, Component.empty(), true, secondary -> {
				option.setPending(option.cycle(option.getPending(), secondary ? -1 : 1));
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
			setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.button.setMessage(this.option.valueText(this.option.getPending()));
		}
	}

	private static final class ColorControl implements OptionControl {
		private final ColorSwatch swatch;
		private final TextControl<Integer> text;

		ColorControl(Font font, ColorOption option, OptionHost host) {
			this.text = new TextControl<>(font, option, value -> ColorOption.toHex(value, option.hasAlpha()), input -> {
				Integer parsed = ColorOption.parseHex(input);
				if (parsed == null) {
					return null;
				}
				return option.hasAlpha() ? parsed : parsed | 0xFF000000;
			});
			this.swatch = new ColorSwatch(option::getPending, () -> host.openSubScreen(
				new ColorPickerScreen(host.asScreen(), option.getName(), option.getPending(), option.hasAlpha(), picked -> {
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
			setBounds(this.swatch, x, y, height, height);
			this.text.layout(x + height + 4, y, width - height - 4, height);
		}

		@Override
		public void refresh() {
			this.text.refresh();
		}
	}

	public static final class KeyBindControl implements OptionControl {
		private final KeyBindOption option;
		private final TabbyButton button;
		private final OptionHost host;

		KeyBindControl(KeyBindOption option, OptionHost host) {
			this.option = option;
			this.host = host;
			this.button = new TabbyButton(0, 20, Component.empty(), () -> {
				host.startKeyCapture(this);
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
			setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			InputConstants.Key key = this.option.getPending();
			boolean capturing = this.host.isCapturing(this);
			MutableComponent text = key.getDisplayName().copy();
			if (capturing) {
				text = Component.literal("> ").append(text.withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <").withStyle(ChatFormatting.YELLOW);
			}
			List<Component> conflicts = this.conflicts(key);
			if (!conflicts.isEmpty() && !capturing) {
				text = Component.literal("[ ").append(text).append(" ]").withStyle(ChatFormatting.RED);
				this.button.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds",
					ComponentUtils.formatList(conflicts, Component.literal(", ")))));
			} else {
				this.button.setTooltip(null);
			}
			this.button.setMessage(text);
		}

		private List<Component> conflicts(InputConstants.Key key) {
			if (key.equals(InputConstants.UNKNOWN)) {
				return List.of();
			}
			KeyMapping own = this.option.getMapping();
			boolean ownDefault = key.getName().equals(own.getDefaultKey().getName());
			List<Component> result = new ArrayList<>();
			for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
				if (other != own && other.saveString().equals(key.getName()) && !(ownDefault && other.isDefault())) {
					result.add(Component.translatable(other.getName()));
				}
			}
			return result;
		}
	}

	private static final class ButtonControl implements OptionControl {
		private final Supplier<Component> text;
		private final TabbyButton button;

		ButtonControl(Supplier<Component> text, Runnable onPress) {
			this.text = text;
			this.button = new TabbyButton(0, 20, text.get(), onPress);
		}

		@Override
		public List<? extends AbstractWidget> widgets() {
			return List.of(this.button);
		}

		@Override
		public void layout(int x, int y, int width, int height) {
			setBounds(this.button, x, y, width, height);
		}

		@Override
		public void refresh() {
			this.button.setMessage(this.text.get());
		}
	}
}
