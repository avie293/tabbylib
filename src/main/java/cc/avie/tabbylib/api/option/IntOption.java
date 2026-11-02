package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** Whole number, created with {@link Options#integer}. */
public class IntOption extends Option<Integer, IntOption> {
	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;
	private int step = 1;
	private boolean slider;
	private @Nullable Function<Integer, Component> formatter;

	protected IntOption(String key, int defaultValue) {
		super(key, defaultValue);
	}

	/** Allowed range, edited with a text field. */
	public IntOption range(int min, int max) {
		return this.configure(() -> {
			this.min = min;
			this.max = max;
		});
	}

	/** Allowed range, edited with a slider that snaps to multiples of the step (counted from the minimum). */
	public IntOption slider(int min, int max, int step) {
		return this.configure(() -> {
			this.min = min;
			this.max = max;
			this.step = Math.max(1, step);
			this.slider = true;
		});
	}

	/** How the value is shown on the slider. */
	public IntOption format(Function<Integer, Component> formatter) {
		return this.configure(() -> this.formatter = formatter);
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

	/** Whether the screen shows a slider (else a text field). */
	public boolean isSlider() {
		return this.slider;
	}

	/** Text shown for a value, from the {@link #format} function if one is set. */
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
}
