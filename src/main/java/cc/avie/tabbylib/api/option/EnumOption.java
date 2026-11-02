package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

/** One constant of an enum, created with {@link Options#choice}. */
public class EnumOption<E extends Enum<E>> extends Option<E, EnumOption<E>> {
	private final Class<E> enumClass;
	private @Nullable Function<E, Component> formatter;

	protected EnumOption(String key, E defaultValue) {
		super(key, defaultValue);
		this.enumClass = defaultValue.getDeclaringClass();
	}

	/** Own display text per constant instead of translation keys. */
	public EnumOption<E> format(Function<E, Component> formatter) {
		return this.configure(() -> this.formatter = formatter);
	}

	/** All constants in declaration order. */
	public E[] values() {
		return this.enumClass.getEnumConstants();
	}

	/** The next (direction 1) or previous (direction -1) constant, wrapping around at the ends. */
	public E cycle(E current, int direction) {
		E[] values = this.values();
		return values[Math.floorMod(current.ordinal() + direction, values.length)];
	}

	/** Display text of a constant. */
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
}
