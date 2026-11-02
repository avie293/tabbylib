package cc.avie.tabbylib.client.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class ValueSlider extends AbstractSliderButton {
	private final double min;
	private final double max;
	private final double step;
	private final DoubleFunction<Component> formatter;
	private final DoubleConsumer onChange;

	public ValueSlider(double min, double max, double step, double initial, DoubleFunction<Component> formatter, DoubleConsumer onChange) {
		super(0, 0, 0, 20, Component.empty(), 0);
		this.min = min;
		this.max = max;
		this.step = step;
		this.formatter = formatter;
		this.onChange = onChange;
		this.setActualValue(initial);
	}

	public double getActualValue() {
		double raw = this.min + this.value * (this.max - this.min);
		if (this.step > 0) {
			raw = this.min + Math.round((raw - this.min) / this.step) * this.step;
		}
		return Mth.clamp(raw, this.min, this.max);
	}

	public void setActualValue(double actual) {
		this.value = this.max > this.min ? Mth.clamp((actual - this.min) / (this.max - this.min), 0, 1) : 0;
		this.updateMessage();
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.formatter.apply(this.getActualValue()));
	}

	@Override
	protected void applyValue() {
		this.onChange.accept(this.getActualValue());
	}
}
