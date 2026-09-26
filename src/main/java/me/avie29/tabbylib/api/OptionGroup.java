package me.avie29.tabbylib.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collapsible section inside a category.
 * The name is translated as {@code config.<modid>.group.<key>} unless set explicitly.
 */
public class OptionGroup implements ConfigEntry {
	private final String key;
	private final List<ConfigEntry> entries = new ArrayList<>();
	private @Nullable Component name;
	private @Nullable Component description;
	private boolean collapsed;
	private @Nullable TabbyConfig config;

	public OptionGroup(String key) {
		this.key = key;
	}

	/** Key of the group, used in its translation key. */
	public String getKey() {
		return this.key;
	}

	/**
	 * Display name: the one set with {@link #name}, otherwise the translation of
	 * {@code config.<translation id>.group.<key>}.
	 */
	public Component getName() {
		if (this.name != null) {
			return this.name;
		}
		String modId = this.config != null ? this.config.getTranslationId() : "unknown";
		return Component.translatable("config." + modId + ".group." + this.key);
	}

	/** Uses a fixed name instead of the translation key. */
	public OptionGroup name(Component name) {
		this.name = name;
		return this;
	}

	/** Text shown when hovering the group header. */
	public OptionGroup description(Component description) {
		this.description = description;
		return this;
	}

	/** Hover text of the group header, or null. */
	public @Nullable Component getDescription() {
		return this.description;
	}

	/** Starts collapsed when the screen is opened. */
	public OptionGroup collapsed() {
		this.collapsed = true;
		return this;
	}

	/** Whether the group is collapsed in the config screen right now. */
	public boolean isCollapsed() {
		return this.collapsed;
	}

	/** Collapses or expands the group. The screen keeps this while the game is running. */
	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	/** Adds an option, label or action. Groups can not contain other groups. */
	public OptionGroup add(ConfigEntry entry) {
		if (entry instanceof OptionGroup) {
			throw new IllegalArgumentException("Groups can not be nested");
		}
		this.entries.add(entry);
		return this;
	}

	/** Adds several entries in the given order. */
	public OptionGroup add(ConfigEntry... entries) {
		for (ConfigEntry entry : entries) {
			this.add(entry);
		}
		return this;
	}

	/** Adds a line of text, e.g. an explanation. */
	public OptionGroup label(Component text) {
		return this.add(new LabelEntry(text));
	}

	/** The entries in display order. */
	public List<ConfigEntry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	void attach(TabbyConfig config) {
		this.config = config;
	}
}
