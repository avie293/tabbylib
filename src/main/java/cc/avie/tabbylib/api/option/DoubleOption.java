package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

/** Decimal number, created with {@link Options#decimal}. */
public class DoubleOption extends Option<Double, DoubleOption> {
	private double min = -Double.MAX_VALUE;
	private double max = Double.MAX_VALUE;
	private double step;
	private boolean slider;
	private @Nullable Function<Double, Component> formatter;

	protected DoubleOption(String key, double defaultValue) {
		super(key, defaultValue);
	}

	/** Allowed range, edited with a text field. */
	public DoubleOption range(double min, double max) {
		return this.configure(() -> {
			this.min = min;
			this.max = max;
		});
	}

	/** Allowed range, edited with a slider that snaps to multiples of the step (0 for no snapping). */
	public DoubleOption slider(double min, double max, double step) {
		return this.configure(() -> {
			this.min = min;
			this.max = max;
			this.step = step;
			this.slider = true;
		});
	}

	/** How the value is shown on the slider. */
	public DoubleOption format(Function<Double, Component> formatter) {
		return this.configure(() -> this.formatter = formatter);
	}

	public double getMin() {
		return this.min;
	}

	public double getMax() {
		return this.max;
	}

	public double getStep() {
		return this.step;
	}

	/** Whether the screen shows a slider (else a text field). */
	public boolean isSlider() {
		return this.slider;
	}

	/** Text shown for a value, from the {@link #format} function if one is set. */
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
}
