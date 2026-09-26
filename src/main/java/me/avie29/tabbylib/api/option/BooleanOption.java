package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** On / off toggle. */
public class BooleanOption extends Option<Boolean> {
	private final @Nullable Component trueText;
	private final @Nullable Component falseText;

	protected BooleanOption(Builder builder) {
		super(builder);
		this.trueText = builder.trueText;
		this.falseText = builder.falseText;
	}

	/**
	 * Starts a new toggle.
	 *
	 * @param key unique key inside the config, used in the file and in the translation key
	 */
	public static Builder builder(String key, boolean defaultValue) {
		return new Builder(key, defaultValue);
	}

	/** Text shown on the toggle button, e.g. "Shown" / "Hidden". Defaults to On / Off. */
	public Component valueText(boolean value) {
		if (value) {
			return this.trueText != null ? this.trueText : Component.translatable("tabbylib.value.on");
		}
		return this.falseText != null ? this.falseText : Component.translatable("tabbylib.value.off");
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

	/** Builder for {@link BooleanOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder extends Option.Builder<Boolean, Builder> {
		private @Nullable Component trueText;
		private @Nullable Component falseText;

		protected Builder(String key, boolean defaultValue) {
			super(key, defaultValue);
		}

		/** Text on the button instead of "On" / "Off", e.g. "Shown" / "Hidden". */
		public Builder valueText(Component trueText, Component falseText) {
			this.trueText = trueText;
			this.falseText = falseText;
			return this;
		}

		@Override
		public BooleanOption build() {
			return new BooleanOption(this);
		}
	}
}
