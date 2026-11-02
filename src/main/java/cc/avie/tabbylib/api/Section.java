package cc.avie.tabbylib.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A collapsible part of a {@link Category}, created with {@link Category#section}. */
public final class Section implements ConfigEntry {
	private final String key;
	private final List<ConfigEntry> entries = new ArrayList<>();
	private @Nullable Component name;
	private @Nullable Component description;
	private boolean collapsed;
	private @Nullable TabbyConfig config;

	Section(String key) {
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
		return Component.translatable("config." + translationId + ".section." + this.key);
	}

	/** Uses a fixed name instead of the translation key. */
	public Section name(Component name) {
		this.name = name;
		return this;
	}

	/** Text shown when hovering the section header. */
	public Section description(Component description) {
		this.description = description;
		return this;
	}

	public @Nullable Component getDescription() {
		return this.description;
	}

	/** Starts collapsed. */
	public Section collapsed() {
		this.collapsed = true;
		return this;
	}

	/** Whether the section is collapsed in the screen right now. */
	public boolean isCollapsed() {
		return this.collapsed;
	}

	/** Collapses or expands the section. */
	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	/** Adds options, labels or actions in the given order. */
	public Section add(ConfigEntry... entries) {
		for (ConfigEntry entry : entries) {
			if (entry instanceof Section) {
				throw new IllegalArgumentException("Sections can not be nested");
			}
			this.entries.add(entry);
		}
		return this;
	}

	/** Adds a line of text. */
	public Section label(Component text) {
		return this.add(new Label(text));
	}

	/** Adds a button that runs code. */
	public Section action(Component name, Component buttonText, Runnable action) {
		return this.add(Action.of(name, buttonText, action));
	}

	public List<ConfigEntry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	void attach(TabbyConfig config) {
		this.config = config;
	}
}
