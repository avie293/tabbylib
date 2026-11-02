package cc.avie.tabbylib.api.option;

import cc.avie.tabbylib.api.HudPosition;
import cc.avie.tabbylib.api.HudPreview;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.function.Supplier;

/** Creates options. */
public final class Options {
	private Options() {
	}

	/** On / off button. */
	public static BooleanOption toggle(String key, boolean defaultValue) {
		return new BooleanOption(key, defaultValue);
	}

	/** Whole number, a text field unless {@link IntOption#slider} is used. */
	public static IntOption integer(String key, int defaultValue) {
		return new IntOption(key, defaultValue);
	}

	/** Decimal number, a text field unless {@link DoubleOption#slider} is used. */
	public static DoubleOption decimal(String key, double defaultValue) {
		return new DoubleOption(key, defaultValue);
	}

	/** Free text. */
	public static StringOption text(String key, String defaultValue) {
		return new StringOption(key, defaultValue);
	}

	/** One constant of an enum, a button that cycles through them. */
	public static <E extends Enum<E>> EnumOption<E> choice(String key, E defaultValue) {
		return new EnumOption<>(key, defaultValue);
	}

	/** Opaque color as {@code 0xRRGGBB} (the alpha byte is ignored), edited with a hex field and a color picker. */
	public static ColorOption color(String key, int defaultColor) {
		return new ColorOption(key, defaultColor, false);
	}

	/** Color with transparency as {@code 0xAARRGGBB}. */
	public static ColorOption colorWithAlpha(String key, int defaultColor) {
		return new ColorOption(key, defaultColor, true);
	}

	/** A vanilla key mapping, so players can rebind it in the config screen. */
	public static KeyBindOption keyBind(String key, KeyMapping mapping) {
		return new KeyBindOption(key, mapping);
	}

	/** Editable list of strings. */
	public static StringListOption textList(String key, List<String> defaultValue) {
		return new StringListOption(key, defaultValue);
	}

	/** Position of a HUD element, edited by drag and drop. */
	public static HudPositionOption hudPosition(String key, HudPosition defaultValue, Supplier<HudPreview> preview) {
		return new HudPositionOption(key, defaultValue, preview);
	}
}
