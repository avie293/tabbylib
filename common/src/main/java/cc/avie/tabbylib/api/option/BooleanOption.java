package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** On / off toggle, created with {@link Options#toggle}. */
public class BooleanOption extends Option<Boolean, BooleanOption> {
	private @Nullable Component onText;
	private @Nullable Component offText;

	protected BooleanOption(String key, boolean defaultValue) {
		super(key, defaultValue);
	}

	/** Text on the button instead of "On" / "Off". */
	public BooleanOption labels(Component onText, Component offText) {
		return this.configure(() -> {
			this.onText = onText;
			this.offText = offText;
		});
	}

	/** Text shown on the button for a value. */
	public Component valueText(boolean value) {
		if (value) {
			return this.onText != null ? this.onText : Component.translatable("tabbylib.value.on");
		}
		return this.offText != null ? this.offText : Component.translatable("tabbylib.value.off");
	}

	@Override
	public String formatValue(Boolean value) {
		return this.valueText(value).getString();
	}

	@Override
	public JsonElement toJson(Boolean value) {
		return new JsonPrimitive(value);
	}

	@Override
	public Boolean fromJson(JsonElement json) {
		return json.getAsBoolean();
	}
}
