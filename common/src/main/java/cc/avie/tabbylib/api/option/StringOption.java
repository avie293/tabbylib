package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** Free text, created with {@link Options#text}. */
public class StringOption extends Option<String, StringOption> {
	private int maxLength = 256;
	private @Nullable Predicate<String> validator;
	private @Nullable Component hint;

	protected StringOption(String key, String defaultValue) {
		super(key, defaultValue);
	}

	/** Longest allowed text (default 256). */
	public StringOption maxLength(int maxLength) {
		return this.configure(() -> this.maxLength = maxLength);
	}

	/** Only text that passes the check is applied, invalid input is shown in red. */
	public StringOption validator(Predicate<String> validator) {
		return this.configure(() -> this.validator = validator);
	}

	/** Grey placeholder shown while the field is empty. */
	public StringOption hint(Component hint) {
		return this.configure(() -> this.hint = hint);
	}

	public int getMaxLength() {
		return this.maxLength;
	}

	public @Nullable Component getHint() {
		return this.hint;
	}

	/** Whether the text may be applied. */
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
}
