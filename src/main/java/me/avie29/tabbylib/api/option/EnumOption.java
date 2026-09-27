package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

/**
 * One value out of an enum. Left click cycles forward, right click backwards.
 * Enum constants are translated as {@code config.<modid>.<key>.<constant in lowercase>} unless a formatter is set.
 */
public class EnumOption<E extends Enum<E>> extends Option<E> {
	private final Class<E> enumClass;
	private final @Nullable Function<E, Component> formatter;

	protected EnumOption(Builder<E> builder) {
		super(builder);
		this.enumClass = builder.enumClass;
		this.formatter = builder.formatter;
	}

	/**
	 * Starts a new enum option. The enum type is taken from the default value.
	 *
	 * @param key unique key inside the config, used in the file and in the translation key
	 */
	public static <E extends Enum<E>> Builder<E> builder(String key, E defaultValue) {
		return new Builder<>(key, defaultValue);
	}

	/** All constants of the enum, in declaration order. */
	public E[] values() {
		return this.enumClass.getEnumConstants();
	}

	/** The next (direction 1) or previous (direction -1) constant, wrapping around at the ends. */
	public E cycle(E current, int direction) {
		E[] values = this.values();
		return values[Math.floorMod(current.ordinal() + direction, values.length)];
	}

	/** Display text of a constant: from the {@link Builder#formatter}, the translation key or the constant name. */
	public Component valueText(E value) {
		if (this.formatter != null) {
			return this.formatter.apply(value);
		}
		String key = this.translationKey() + "." + value.name().toLowerCase(Locale.ROOT);
		return TranslationHelper.exists(key) ? Component.translatable(key) : Component.literal(prettify(value.name()));
	}

	private static String prettify(String name) {
		String lower = name.replace('_', ' ').toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	@Override
	public String formatValue(E value) {
		return this.valueText(value).getString();
	}

	@Override
	public JsonElement toJson(E value) {
		return new JsonPrimitive(value.name());
	}

	@Override
	public E fromJson(JsonElement json) {
		return Enum.valueOf(this.enumClass, json.getAsString());
	}

	/** Builder for {@link EnumOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder<E extends Enum<E>> extends Option.Builder<E, Builder<E>> {
		private final Class<E> enumClass;
		private @Nullable Function<E, Component> formatter;

		protected Builder(String key, E defaultValue) {
			super(key, defaultValue);
			this.enumClass = defaultValue.getDeclaringClass();
		}

		/** Custom display text per constant instead of translation keys. */
		public Builder<E> formatter(Function<E, Component> formatter) {
			this.formatter = formatter;
			return this;
		}

		@Override
		public EnumOption<E> build() {
			return new EnumOption<>(this);
		}
	}
}
