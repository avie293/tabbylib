package cc.avie.tabbylib.api;

import cc.avie.tabbylib.api.option.Option;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** A tab in the config screen of a mod, created with {@link TabbyConfig.Builder#category}. */
public final class Category {
	private final String key;
	private final List<ConfigEntry> entries = new ArrayList<>();
	private @Nullable Component name;
	private @Nullable String fileName;
	private @Nullable TabbyConfig config;

	Category(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	public Component getName() {
		if (this.name != null) {
			return this.name;
		}
		String translationId = this.config != null ? this.config.getTranslationId() : "unknown";
		return Component.translatable("config." + translationId + ".category." + this.key);
	}

	/** Uses a fixed name instead of the translation key. */
	public Category name(Component name) {
		this.name = name;
		return this;
	}

	/** Saves the options of this category in their own file, {@code config/tabbylib/<file name>.json}. */
	public Category file(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/** The own file set with {@link #file}, or null. */
	public @Nullable String getFileName() {
		return this.fileName;
	}

	/** Adds options, sections, labels or actions in the given order. */
	public Category add(ConfigEntry... entries) {
		Collections.addAll(this.entries, entries);
		return this;
	}

	/** Adds a collapsible section: {@code .section("look", section -> section.add(COLOR, SIZE))}. */
	public Category section(String key, Consumer<Section> content) {
		Section section = new Section(key);
		content.accept(section);
		return this.add(section);
	}

	/** Adds a line of text. */
	public Category label(Component text) {
		return this.add(new Label(text));
	}

	/** Adds a button that runs code. */
	public Category action(Component name, Component buttonText, Runnable action) {
		return this.add(Action.of(name, buttonText, action));
	}

	/** The entries in display order, sections as one entry. */
	public List<ConfigEntry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	/** All options of this category, including the ones inside sections. */
	public List<Option<?, ?>> getOptions() {
		List<Option<?, ?>> options = new ArrayList<>();
		for (ConfigEntry entry : this.entries) {
			if (entry instanceof Option<?, ?> option) {
				options.add(option);
			} else if (entry instanceof Section section) {
				for (ConfigEntry inner : section.getEntries()) {
					if (inner instanceof Option<?, ?> option) {
						options.add(option);
					}
				}
			}
		}
		return options;
	}

	void attach(TabbyConfig config) {
		this.config = config;
		for (ConfigEntry entry : this.entries) {
			if (entry instanceof Section section) {
				section.attach(config);
			}
		}
		for (Option<?, ?> option : this.getOptions()) {
			option.attach(config);
		}
	}
}
