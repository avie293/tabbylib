package cc.avie.tabbylib.api.option;

import cc.avie.tabbylib.api.ConfigEntry;
import cc.avie.tabbylib.api.TabbyConfig;
import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A single configurable value. */
public abstract class Option<T, S extends Option<T, S>> implements ConfigEntry {
	private final String key;
	private final T defaultValue;
	private final List<Consumer<T>> changeListeners = new ArrayList<>();
	private @Nullable Supplier<T> getter;
	private @Nullable Consumer<T> setter;
	private @Nullable BooleanSupplier enabledWhen;
	private @Nullable BooleanSupplier visibleWhen;
	private boolean requiresRestart;
	private boolean hidden;
	private @Nullable Component customName;
	private @Nullable Component customTooltip;

	private T value;
	private @Nullable T pending;
	private boolean editing;
	private @Nullable TabbyConfig config;
	private @Nullable String translationKey;

	protected Option(String key, T defaultValue) {
		this.key = Objects.requireNonNull(key, "key");
		this.defaultValue = Objects.requireNonNull(defaultValue, () -> "default value of " + key);
		this.value = defaultValue;
	}

	@SuppressWarnings("unchecked")
	protected final S self() {
		return (S) this;
	}

	/** Settings may only change while the option is not part of a config yet. */
	protected final S configure(Runnable change) {
		if (this.config != null) {
			throw new IllegalStateException("Option '" + this.key + "' can not be changed after it was added to a config");
		}
		change.run();
		return this.self();
	}

	/** The value your mod should use. */
	public T get() {
		return this.getter != null ? this.getter.get() : this.value;
	}

	/** Changes the saved value in code. */
	public void set(T newValue) {
		T validated = this.validate(newValue);
		T old = this.get();
		this.store(validated);
		if (!this.valueEquals(old, validated)) {
			for (Consumer<T> listener : this.changeListeners) {
				listener.accept(validated);
			}
		}
	}

	/** Sets the saved value without notifying change listeners. */
	public void loadValue(T newValue) {
		this.store(this.validate(newValue));
	}

	private void store(T newValue) {
		if (this.setter != null) {
			this.setter.accept(newValue);
		} else {
			this.value = newValue;
		}
	}

	/** The default value given to the factory method. */
	public T getDefault() {
		return this.defaultValue;
	}

	/** The value currently shown in the config screen, including unsaved changes. */
	public T getPending() {
		return this.editing ? Objects.requireNonNull(this.pending) : this.get();
	}

	/** Changes the value shown in the config screen. */
	public void setPending(T newValue) {
		this.pending = this.validate(newValue);
		this.editing = true;
	}

	/** Sets the pending value back to the default. */
	public void resetPending() {
		this.setPending(this.defaultValue);
	}

	/** Whether the pending value differs from the saved value. */
	public boolean isDirty() {
		return this.editing && !this.valueEquals(this.pending, this.get());
	}

	/** Whether the pending value is the default value. */
	public boolean isPendingDefault() {
		return this.valueEquals(this.getPending(), this.defaultValue);
	}

	/** Applies the pending value. */
	public boolean commit() {
		boolean changed = this.isDirty();
		if (changed) {
			this.set(Objects.requireNonNull(this.pending));
		}
		this.discard();
		return changed;
	}

	/** Forgets the pending value, {@link #getPending()} returns {@link #get()} again. */
	public void discard() {
		this.pending = null;
		this.editing = false;
	}

	/** Key of the option: its name in the config file and the last part of its translation key. */
	public String getKey() {
		return this.key;
	}

	/** Display name: the one set with {@link #name}, otherwise the translation of {@link #translationKey()}. */
	public Component getName() {
		return this.customName != null ? this.customName : Component.translatable(this.translationKey());
	}

	/** Hover text: the one set with {@link #tooltip}, otherwise the translation of {@code <translation key>.tooltip} when it exists, else null. */
	public @Nullable Component getTooltip() {
		if (this.customTooltip != null) {
			return this.customTooltip;
		}
		String tooltipKey = this.translationKey() + ".tooltip";
		return TranslationHelper.exists(tooltipKey) ? Component.translatable(tooltipKey) : null;
	}

	/** {@code config.<translation id>.<key>}, see {@link TabbyConfig.Builder#translationId}. */
	public String translationKey() {
		if (this.config == null) {
			return "config.unknown." + this.key;
		}
		if (this.translationKey == null) {
			this.translationKey = "config." + this.config.getTranslationId() + "." + this.key;
		}
		return this.translationKey;
	}

	/** False while the {@link #enabledWhen} condition is not met. */
	public boolean isEnabled() {
		return this.enabledWhen == null || this.enabledWhen.getAsBoolean();
	}

	/** False for {@link #hidden} options and while the {@link #visibleWhen} condition is not met. */
	public boolean isVisible() {
		return !this.hidden && (this.visibleWhen == null || this.visibleWhen.getAsBoolean());
	}

	/** Whether changing this option needs a game restart. */
	public boolean isRestartRequired() {
		return this.requiresRestart;
	}

	/** The config this option belongs to, null until it was added to one. */
	public @Nullable TabbyConfig getConfig() {
		return this.config;
	}

	/** Called by {@link TabbyConfig} when the option is added. */
	public void attach(TabbyConfig config) {
		if (this.config != null && this.config != config) {
			throw new IllegalStateException("Option '" + this.key + "' is already part of the config of " + this.config.getModId());
		}
		this.config = config;
	}

	/** Uses a fixed name instead of the translation key {@code config.<translation id>.<key>}. */
	public S name(Component name) {
		return this.configure(() -> this.customName = name);
	}

	/** Uses a fixed hover text instead of the translation key {@code config.<translation id>.<key>.tooltip}. */
	public S tooltip(Component tooltip) {
		return this.configure(() -> this.customTooltip = tooltip);
	}

	/** Stores the value in an existing field of your mod instead of in the option. */
	public S bind(Supplier<T> getter, Consumer<T> setter) {
		return this.configure(() -> {
			this.getter = getter;
			this.setter = setter;
		});
	}

	/** Runs after the saved value changed, on "Save" or through {@link #set}. */
	public S onChange(Consumer<T> listener) {
		return this.configure(() -> this.changeListeners.add(listener));
	}

	/** Greys the option out while the condition is false. */
	public S enabledWhen(BooleanSupplier condition) {
		return this.configure(() -> this.enabledWhen = condition);
	}

	/** Only enabled while the given toggle is (pending) on. */
	public S dependsOn(BooleanOption toggle) {
		return this.enabledWhen(toggle::getPending);
	}

	/** Hides the option in the screen while the condition is false. */
	public S visibleWhen(BooleanSupplier condition) {
		return this.configure(() -> this.visibleWhen = condition);
	}

	/** Marks the option with a red star and shows a restart hint after saving. */
	public S requiresRestart() {
		return this.configure(() -> this.requiresRestart = true);
	}

	/** Saved in the config file but never shown in the screen. */
	public S hidden() {
		return this.configure(() -> this.hidden = true);
	}

	/** Clamps or fixes a value. */
	protected T validate(T value) {
		return value;
	}

	protected boolean valueEquals(@Nullable T a, @Nullable T b) {
		return Objects.equals(a, b);
	}

	/** Serializes a value, or returns null when this option is not stored in the config file. */
	public abstract @Nullable JsonElement toJson(T value);

	/** Parses a stored value. */
	public abstract T fromJson(JsonElement json);

	/** Short text for a value. */
	public String formatValue(T value) {
		return String.valueOf(value);
	}
}
