package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** Free text with optional validation. */
public class StringOption extends Option<String> {
	private final int maxLength;
	private final @Nullable Predicate<String> validator;
	private final @Nullable Component hint;

	protected StringOption(Builder builder) {
		super(builder);
		this.maxLength = builder.maxLength;
		this.validator = builder.validator;
		this.hint = builder.hint;
	}

	/**
	 * Starts a new text option.
	 *
	 * @param key unique key inside the config, used in the file and in the translation key
	 */
	public static Builder builder(String key, String defaultValue) {
		return new Builder(key, defaultValue);
	}

	/** Longest allowed text. */
	public int getMaxLength() {
		return this.maxLength;
	}

	/** Grey placeholder shown while the field is empty, or null. */
	public @Nullable Component getHint() {
		return this.hint;
	}

	/** Whether the text may be saved. Invalid input is shown red and not applied. */
	public boolean isValid(String value) {
		return value.length() <= this.maxLength && (this.validator == null || this.validator.test(value));
	}

	@Override
	protected String validate(String value) {
		return this.isValid(value) ? value : this.getDefault();
	}

	@Override
	public JsonElement toJson(String value) {
		return new JsonPrimitive(value);
	}

	@Override
	public String fromJson(JsonElement json) {
		return json.getAsString();
	}

	/** Builder for {@link StringOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder extends Option.Builder<String, Builder> {
		private int maxLength = 256;
		private @Nullable Predicate<String> validator;
		private @Nullable Component hint;

		protected Builder(String key, String defaultValue) {
			super(key, defaultValue);
		}

		/** Limits the length of the text (default 256). */
		public Builder maxLength(int maxLength) {
			this.maxLength = maxLength;
			return this;
		}

		/** Only text that passes the check is saved; invalid input is shown in red. */
		public Builder validator(Predicate<String> validator) {
			this.validator = validator;
			return this;
		}

		/** Grey placeholder text shown while the field is empty. */
		public Builder hint(Component hint) {
			this.hint = hint;
			return this;
		}

		@Override
		public StringOption build() {
			return new StringOption(this);
		}
	}
}
