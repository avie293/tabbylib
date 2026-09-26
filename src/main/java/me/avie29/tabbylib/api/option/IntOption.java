package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** Whole number, shown as slider or as text field. */
public class IntOption extends Option<Integer> {
	private final int min;
	private final int max;
	private final int step;
	private final boolean slider;
	private final @Nullable Function<Integer, Component> formatter;

	protected IntOption(Builder builder) {
		super(builder);
		this.min = builder.min;
		this.max = builder.max;
		this.step = Math.max(1, builder.step);
		this.slider = builder.slider;
		this.formatter = builder.formatter;
	}

	public static Builder builder(String key, int defaultValue) {
		return new Builder(key, defaultValue);
	}

	public int getMin() {
		return this.min;
	}

	public int getMax() {
		return this.max;
	}

	public int getStep() {
		return this.step;
	}

	public boolean isSlider() {
		return this.slider;
	}

	public Component valueText(int value) {
		return this.formatter != null ? this.formatter.apply(value) : Component.literal(Integer.toString(value));
	}

	@Override
	protected Integer validate(Integer value) {
		int clamped = Mth.clamp(value, this.min, this.max);
		if (this.step > 1) {
			clamped = Mth.clamp(this.min + Math.round((clamped - this.min) / (float) this.step) * this.step, this.min, this.max);
		}
		return clamped;
	}

	@Override
	public JsonElement toJson(Integer value) {
		return new JsonPrimitive(value);
	}

	@Override
	public Integer fromJson(JsonElement json) {
		return json.getAsInt();
	}

	public static class Builder extends Option.Builder<Integer, Builder> {
		private int min = Integer.MIN_VALUE;
		private int max = Integer.MAX_VALUE;
		private int step = 1;
		private boolean slider;
		private @Nullable Function<Integer, Component> formatter;

		protected Builder(String key, int defaultValue) {
			super(key, defaultValue);
		}

		/** Allowed range, edited with a text field. */
		public Builder range(int min, int max) {
			this.min = min;
			this.max = max;
			return this;
		}

		/** Allowed range, edited with a slider. */
		public Builder slider(int min, int max, int step) {
			this.range(min, max);
			this.step = step;
			this.slider = true;
			return this;
		}

		/** How the value is displayed on the slider, e.g. {@code v -> Component.literal(v + " ms")}. */
		public Builder formatter(Function<Integer, Component> formatter) {
			this.formatter = formatter;
			return this;
		}

		@Override
		public IntOption build() {
			return new IntOption(this);
		}
	}
}
