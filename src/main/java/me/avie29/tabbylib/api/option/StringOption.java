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

	public static Builder builder(String key, String defaultValue) {
		return new Builder(key, defaultValue);
	}

	public int getMaxLength() {
		return this.maxLength;
	}

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

	public static class Builder extends Option.Builder<String, Builder> {
		private int maxLength = 256;
		private @Nullable Predicate<String> validator;
		private @Nullable Component hint;

		protected Builder(String key, String defaultValue) {
			super(key, defaultValue);
		}

		public Builder maxLength(int maxLength) {
			this.maxLength = maxLength;
			return this;
		}

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
