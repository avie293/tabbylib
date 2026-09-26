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

	public String getKey() {
		return this.key;
	}

	public Component getName() {
		if (this.name != null) {
			return this.name;
		}
		String modId = this.config != null ? this.config.getTranslationId() : "unknown";
		return Component.translatable("config." + modId + ".group." + this.key);
	}

	public OptionGroup name(Component name) {
		this.name = name;
		return this;
	}

	/** Text shown when hovering the group header. */
	public OptionGroup description(Component description) {
		this.description = description;
		return this;
	}

	public @Nullable Component getDescription() {
		return this.description;
	}

	/** Starts collapsed when the screen is opened. */
	public OptionGroup collapsed() {
		this.collapsed = true;
		return this;
	}

	public boolean isCollapsed() {
		return this.collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	public OptionGroup add(ConfigEntry entry) {
		if (entry instanceof OptionGroup) {
			throw new IllegalArgumentException("Groups can not be nested");
		}
		this.entries.add(entry);
		return this;
	}

	public OptionGroup add(ConfigEntry... entries) {
		for (ConfigEntry entry : entries) {
			this.add(entry);
		}
		return this;
	}

	public OptionGroup label(Component text) {
		return this.add(new LabelEntry(text));
	}

	public List<ConfigEntry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	void attach(TabbyConfig config) {
		this.config = config;
	}
}
