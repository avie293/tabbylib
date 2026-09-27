package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

/** Decimal number, shown as slider or as text field. */
public class DoubleOption extends Option<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final boolean slider;
	private final @Nullable Function<Double, Component> formatter;

	protected DoubleOption(Builder builder) {
		super(builder);
		this.min = builder.min;
		this.max = builder.max;
		this.step = builder.step;
		this.slider = builder.slider;
		this.formatter = builder.formatter;
	}

	/**
	 * Starts a new decimal number option.
	 *
	 * @param key unique key inside the config, used in the file and in the translation key
	 */
	public static Builder builder(String key, double defaultValue) {
		return new Builder(key, defaultValue);
	}

	/** Smallest allowed value. */
	public double getMin() {
		return this.min;
	}

	/** Largest allowed value. */
	public double getMax() {
		return this.max;
	}

	/** Values snap to multiples of this step (counted from the minimum), 0 for no snapping. */
	public double getStep() {
		return this.step;
	}

	/** Whether the screen shows a slider (else a text field). */
	public boolean isSlider() {
		return this.slider;
	}

	/** Text shown for a value, from the {@link Builder#formatter} if one is set. */
	public Component valueText(double value) {
		return this.formatter != null ? this.formatter.apply(value) : Component.literal(this.formatValue(value));
	}

	@Override
	public String formatValue(Double value) {
		if (value == Math.rint(value)) {
			return Long.toString(Math.round(value));
		}
		return String.format(Locale.ROOT, "%.2f", value);
	}

	@Override
	protected Double validate(Double value) {
		if (value.isNaN()) {
			return this.getDefault();
		}
		double clamped = Mth.clamp(value, this.min, this.max);
		if (this.step > 0) {
			clamped = Mth.clamp(this.min + Math.round((clamped - this.min) / this.step) * this.step, this.min, this.max);
			// Removes floating point noise like 0.30000000000000004
			clamped = Math.round(clamped * 1_000_000d) / 1_000_000d;
		}
		return clamped;
	}

	@Override
	public JsonElement toJson(Double value) {
		return new JsonPrimitive(value);
	}

	@Override
	public Double fromJson(JsonElement json) {
		return json.getAsDouble();
	}

	/** Builder for {@link DoubleOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder extends Option.Builder<Double, Builder> {
		private double min = -Double.MAX_VALUE;
		private double max = Double.MAX_VALUE;
		private double step;
		private boolean slider;
		private @Nullable Function<Double, Component> formatter;

		protected Builder(String key, double defaultValue) {
			super(key, defaultValue);
		}

		/** Allowed range, edited with a text field. */
		public Builder range(double min, double max) {
			this.min = min;
			this.max = max;
			return this;
		}

		/** Allowed range, edited with a slider that snaps to the step. */
		public Builder slider(double min, double max, double step) {
			this.range(min, max);
			this.step = step;
			this.slider = true;
			return this;
		}

		/** How the value is displayed on the slider, e.g. {@code v -> Component.literal(v + "x")}. */
		public Builder formatter(Function<Double, Component> formatter) {
			this.formatter = formatter;
			return this;
		}

		@Override
		public DoubleOption build() {
			return new DoubleOption(this);
		}
	}
}
