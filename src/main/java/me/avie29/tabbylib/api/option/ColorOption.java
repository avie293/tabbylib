package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A color stored as ARGB int. Saved as "#RRGGBB" or "#AARRGGBB" (with alpha enabled).
 * The config screen offers a hex field and a color picker.
 */
public class ColorOption extends Option<Integer> {
	private final boolean alpha;

	protected ColorOption(Builder builder) {
		super(builder);
		this.alpha = builder.alpha;
	}

	/**
	 * Starts a new color option.
	 *
	 * @param key          unique key inside the config, used in the file and in the translation key
	 * @param defaultColor ARGB, e.g. {@code 0xFFFF0000} for opaque red. Without alpha support the alpha byte is forced to FF.
	 */
	public static Builder builder(String key, int defaultColor) {
		return new Builder(key, defaultColor);
	}

	/** Whether the alpha channel (transparency) can be edited. */
	public boolean hasAlpha() {
		return this.alpha;
	}

	@Override
	protected Integer validate(Integer value) {
		return this.alpha ? value : value | 0xFF000000;
	}

	@Override
	public String formatValue(Integer value) {
		return toHex(value, this.alpha);
	}

	/** Formats a color as {@code #RRGGBB}, or {@code #AARRGGBB} with alpha. */
	public static String toHex(int color, boolean alpha) {
		return alpha
			? String.format(Locale.ROOT, "#%08X", color)
			: String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
	}

	/** Parses "#RGB", "#RRGGBB" or "#AARRGGBB" (the # is optional). Returns null for invalid input. */
	public static @Nullable Integer parseHex(String text) {
		String clean = text.trim();
		if (clean.startsWith("#")) {
			clean = clean.substring(1);
		}
		try {
			return switch (clean.length()) {
				case 3 -> {
					int r = Character.digit(clean.charAt(0), 16);
					int g = Character.digit(clean.charAt(1), 16);
					int b = Character.digit(clean.charAt(2), 16);
					if (r < 0 || g < 0 || b < 0) {
						yield null;
					}
					yield 0xFF000000 | (r * 17) << 16 | (g * 17) << 8 | b * 17;
				}
				case 6 -> 0xFF000000 | Integer.parseUnsignedInt(clean, 16);
				case 8 -> Integer.parseUnsignedInt(clean, 16);
				default -> null;
			};
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public JsonElement toJson(Integer value) {
		return new JsonPrimitive(toHex(value, this.alpha));
	}

	@Override
	public Integer fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			return json.getAsInt();
		}
		Integer parsed = parseHex(json.getAsString());
		if (parsed == null) {
			throw new IllegalArgumentException("Invalid color: " + json);
		}
		return parsed;
	}

	/** Builder for {@link ColorOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder extends Option.Builder<Integer, Builder> {
		private boolean alpha;

		protected Builder(String key, int defaultColor) {
			super(key, defaultColor);
		}

		/** Allows editing the alpha channel (transparency). */
		public Builder alpha() {
			this.alpha = true;
			return this;
		}

		@Override
		public ColorOption build() {
			if (!this.alpha) {
				this.defaultValue = this.defaultValue | 0xFF000000;
			}
			return new ColorOption(this);
		}
	}
}
