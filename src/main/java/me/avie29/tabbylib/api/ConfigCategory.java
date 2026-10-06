package me.avie29.tabbylib.api;

import me.avie29.tabbylib.api.option.Option;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tab in the config screen. Contains options, groups, labels and action buttons in display order.
 * The name is translated as {@code config.<modid>.category.<key>} unless set explicitly.
 */
public class ConfigCategory {
	private final String key;
	private final List<ConfigEntry> entries = new ArrayList<>();
	private @Nullable Component name;
	private @Nullable String fileName;
	private @Nullable TabbyConfig config;

	public ConfigCategory(String key) {
		this.key = key;
	}

	/** Key of the category, used in its translation key. */
	public String getKey() {
		return this.key;
	}

	/**
	 * Display name: the one set with {@link #name}, otherwise the translation of
	 * {@code config.<translation id>.category.<key>}.
	 */
	public Component getName() {
		if (this.name != null) {
			return this.name;
		}
		String modId = this.config != null ? this.config.getTranslationId() : "unknown";
		return Component.translatable("config." + modId + ".category." + this.key);
	}

	/** Uses a fixed name instead of the translation key. */
	public ConfigCategory name(Component name) {
		this.name = name;
		return this;
	}

	/**
	 * Saves the options of this category in their own file, {@code config/<file name>.json}, instead of the
	 * file of the config. A slash puts it into a folder: {@code file("my-mod/hud")} is
	 * {@code config/my-mod/hud.json}. Values from the old file are taken over the first time.
	 */
	public ConfigCategory file(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/** The own file name set with {@link #file}, or null when the options are in the file of the config. */
	public @Nullable String getFileName() {
		return this.fileName;
	}

	/** Adds an option, group, label or action. */
	public ConfigCategory add(ConfigEntry entry) {
		this.entries.add(entry);
		return this;
	}

	/** Adds several entries in the given order. */
	public ConfigCategory add(ConfigEntry... entries) {
		for (ConfigEntry entry : entries) {
			this.add(entry);
		}
		return this;
	}

	/** Adds a collapsible group of entries. */
	public ConfigCategory group(String key, Consumer<OptionGroup> content) {
		OptionGroup group = new OptionGroup(key);
		content.accept(group);
		return this.add(group);
	}

	/** Adds a line of text, e.g. an explanation. */
	public ConfigCategory label(Component text) {
		return this.add(new LabelEntry(text));
	}

	/** The entries in display order. Groups are returned as one entry, see {@link #getOptions()}. */
	public List<ConfigEntry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	/** All options of this category, including the ones inside groups. */
	public List<Option<?>> getOptions() {
		List<Option<?>> options = new ArrayList<>();
		collectOptions(this.entries, options);
		return options;
	}

	static void collectOptions(List<ConfigEntry> entries, List<Option<?>> out) {
		for (ConfigEntry entry : entries) {
			if (entry instanceof Option<?> option) {
				out.add(option);
			} else if (entry instanceof OptionGroup group) {
				collectOptions(group.getEntries(), out);
			}
		}
	}

	void attach(TabbyConfig config) {
		this.config = config;
		attachEntries(this.entries, config);
	}

	private static void attachEntries(List<ConfigEntry> entries, TabbyConfig config) {
		for (ConfigEntry entry : entries) {
			if (entry instanceof Option<?> option) {
				option.attach(config);
			} else if (entry instanceof OptionGroup group) {
				group.attach(config);
				attachEntries(group.getEntries(), config);
			}
		}
	}
}
