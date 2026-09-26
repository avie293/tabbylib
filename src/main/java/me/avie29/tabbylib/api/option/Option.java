package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import me.avie29.tabbylib.api.ConfigEntry;
import me.avie29.tabbylib.api.TabbyConfig;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A single configurable value.
 * <p>
 * Every option keeps two values: the committed value ({@link #get()}) that the mod uses,
 * and a pending value that the config screen edits. Pending changes are only applied
 * when the user presses "Save" ({@link #commit()}) and can be thrown away with {@link #discard()}.
 * <p>
 * An option can either store its value itself, or be bound to an existing field of your mod
 * via {@link Builder#binding(Supplier, Consumer)}.
 */
public abstract class Option<T> implements ConfigEntry {
	private final String key;
	private final T defaultValue;
	private final @Nullable Supplier<T> getter;
	private final @Nullable Consumer<T> setter;
	private final List<Consumer<T>> changeListeners;
	private final @Nullable BooleanSupplier enabledWhen;
	private final @Nullable BooleanSupplier visibleWhen;
	private final boolean requiresRestart;
	private final boolean hidden;
	private final @Nullable Component customName;
	private final @Nullable Component customTooltip;

	private T value;
	private @Nullable T pending;
	private boolean editing;
	private @Nullable TabbyConfig config;

	protected Option(Builder<T, ?> builder) {
		this.key = Objects.requireNonNull(builder.key, "key");
		this.defaultValue = Objects.requireNonNull(builder.defaultValue, "default value of " + builder.key);
		this.getter = builder.getter;
		this.setter = builder.setter;
		this.changeListeners = List.copyOf(builder.changeListeners);
		this.enabledWhen = builder.enabledWhen;
		this.visibleWhen = builder.visibleWhen;
		this.requiresRestart = builder.requiresRestart;
		this.hidden = builder.hidden;
		this.customName = builder.name;
		this.customTooltip = builder.tooltip;
		this.value = this.defaultValue;
	}

	// ---------------------------------------------------------------- committed value

	/** The value your mod should use. */
	public T get() {
		return this.getter != null ? this.getter.get() : this.value;
	}

	/** Directly changes the committed value (does not save to disk, call {@link TabbyConfig#save()}). */
	public void set(T newValue) {
		T validated = this.validate(newValue);
		T old = this.get();
		if (this.setter != null) {
			this.setter.accept(validated);
		} else {
			this.value = validated;
		}
		if (!this.valueEquals(old, validated)) {
			for (Consumer<T> listener : this.changeListeners) {
				listener.accept(validated);
			}
		}
	}

	/** Sets the committed value without notifying change listeners. Used when loading the config file. */
	public void loadValue(T newValue) {
		T validated = this.validate(newValue);
		if (this.setter != null) {
			this.setter.accept(validated);
		} else {
			this.value = validated;
		}
	}

	public T getDefault() {
		return this.defaultValue;
	}

	// ---------------------------------------------------------------- pending value (config screen)

	/** The value currently shown in the config screen (unsaved). */
	public T getPending() {
		return this.editing ? Objects.requireNonNull(this.pending) : this.get();
	}

	public void setPending(T newValue) {
		this.pending = this.validate(newValue);
		this.editing = true;
	}

	public void resetPending() {
		this.setPending(this.defaultValue);
	}

	public boolean isDirty() {
		return this.editing && !this.valueEquals(this.pending, this.get());
	}

	public boolean isPendingDefault() {
		return this.valueEquals(this.getPending(), this.defaultValue);
	}

	/** Applies the pending value. Returns true when the value changed. */
	public boolean commit() {
		boolean changed = this.isDirty();
		if (changed) {
			this.set(Objects.requireNonNull(this.pending));
		}
		this.discard();
		return changed;
	}

	public void discard() {
		this.pending = null;
		this.editing = false;
	}

	// ---------------------------------------------------------------- meta

	public String getKey() {
		return this.key;
	}

	public Component getName() {
		if (this.customName != null) {
			return this.customName;
		}
		return Component.translatable(this.translationKey());
	}

	public @Nullable Component getTooltip() {
		if (this.customTooltip != null) {
			return this.customTooltip;
		}
		String tooltipKey = this.translationKey() + ".tooltip";
		return TranslationHelper.exists(tooltipKey) ? Component.translatable(tooltipKey) : null;
	}

	public String translationKey() {
		String modId = this.config != null ? this.config.getTranslationId() : "unknown";
		return "config." + modId + "." + this.key;
	}

	public boolean isEnabled() {
		return this.enabledWhen == null || this.enabledWhen.getAsBoolean();
	}

	public boolean isVisible() {
		return !this.hidden && (this.visibleWhen == null || this.visibleWhen.getAsBoolean());
	}

	public boolean requiresRestart() {
		return this.requiresRestart;
	}

	public @Nullable TabbyConfig getConfig() {
		return this.config;
	}

	/** Called by {@link TabbyConfig} when the option is added. */
	public void attach(TabbyConfig config) {
		this.config = config;
	}

	// ---------------------------------------------------------------- type specific

	/** Clamps / fixes a value. The default implementation returns the value unchanged. */
	protected T validate(T value) {
		return value;
	}

	protected boolean valueEquals(@Nullable T a, @Nullable T b) {
		return Objects.equals(a, b);
	}

	/** Serializes the committed value, or returns null when this option is not stored in the config file. */
	public abstract @Nullable JsonElement toJson(T value);

	/** Parses a stored value. Throwing any exception makes the option fall back to its default value. */
	public abstract T fromJson(JsonElement json);

	/** Short text used when showing the value, e.g. in the search results. */
	public String formatValue(T value) {
		return String.valueOf(value);
	}

	// ---------------------------------------------------------------- builder

	@SuppressWarnings("unchecked")
	public abstract static class Builder<T, B extends Builder<T, B>> {
		protected final String key;
		protected T defaultValue;
		private @Nullable Supplier<T> getter;
		private @Nullable Consumer<T> setter;
		private final List<Consumer<T>> changeListeners = new ArrayList<>();
		private @Nullable BooleanSupplier enabledWhen;
		private @Nullable BooleanSupplier visibleWhen;
		private boolean requiresRestart;
		private boolean hidden;
		private @Nullable Component name;
		private @Nullable Component tooltip;

		protected Builder(String key, T defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		protected B self() {
			return (B) this;
		}

		/** Overrides the default display name ({@code config.<modid>.<key>}). */
		public B name(Component name) {
			this.name = name;
			return this.self();
		}

		/** Overrides the default tooltip ({@code config.<modid>.<key>.tooltip}). */
		public B tooltip(Component tooltip) {
			this.tooltip = tooltip;
			return this.self();
		}

		/** Binds the option to an existing field of your mod instead of storing the value itself. */
		public B binding(Supplier<T> getter, Consumer<T> setter) {
			this.getter = getter;
			this.setter = setter;
			return this.self();
		}

		/** Called after the committed value changed (on save or {@link Option#set}). */
		public B onChange(Consumer<T> listener) {
			this.changeListeners.add(listener);
			return this.self();
		}

		/** The option is greyed out while the condition is false. Checked every frame, so use pending values. */
		public B enabledWhen(BooleanSupplier condition) {
			this.enabledWhen = condition;
			return this.self();
		}

		/** Shortcut: only enabled while the given toggle is (pending) on. */
		public B dependsOn(BooleanOption toggle) {
			return this.enabledWhen(toggle::getPending);
		}

		/** The option is hidden from the screen while the condition is false. */
		public B visibleWhen(BooleanSupplier condition) {
			this.visibleWhen = condition;
			return this.self();
		}

		/** Shows a "requires restart" hint and a message after saving. */
		public B requiresRestart() {
			this.requiresRestart = true;
			return this.self();
		}

		/** Saved to the config file but never shown in the screen. */
		public B hidden() {
			this.hidden = true;
			return this.self();
		}

		public abstract Option<T> build();
	}
}
