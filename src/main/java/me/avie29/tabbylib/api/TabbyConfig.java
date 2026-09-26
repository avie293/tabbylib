package me.avie29.tabbylib.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.avie29.tabbylib.TabbyLib;
import me.avie29.tabbylib.api.option.Option;
import me.avie29.tabbylib.platform.ModInfo;
import me.avie29.tabbylib.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The config of one mod. Holds the categories and options, saves them as JSON
 * ({@code config/<fileName>.json}) and shows up in the TabbyLib config screen.
 *
 * <pre>{@code
 * public static final BooleanOption SHOW_HUD = BooleanOption.builder("showHud", true).build();
 *
 * public static final TabbyConfig CONFIG = TabbyConfig.builder("my-mod")
 *     .category("general", category -> category.add(SHOW_HUD))
 *     .build(); // registers the config and loads the file
 * }</pre>
 */
public final class TabbyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final String modId;
	private final String translationId;
	private final Path file;
	private final List<ConfigCategory> categories;
	private final List<Runnable> saveListeners;
	private final List<Predicate<JsonObject>> migrations;
	private final @Nullable Component customName;
	private final @Nullable ResourceLocation customIcon;
	private final Map<String, Option<?>> optionsByKey = new LinkedHashMap<>();

	private TabbyConfig(Builder builder) {
		this.modId = builder.modId;
		this.translationId = builder.translationId;
		this.file = Platform.configDir().resolve(builder.fileName + ".json");
		this.categories = List.copyOf(builder.categories);
		this.saveListeners = List.copyOf(builder.saveListeners);
		this.migrations = List.copyOf(builder.migrations);
		this.customName = builder.name;
		this.customIcon = builder.icon;

		for (ConfigCategory category : this.categories) {
			category.attach(this);
			for (Option<?> option : category.getOptions()) {
				if (this.optionsByKey.put(option.getKey(), option) != null) {
					throw new IllegalStateException("Duplicate option key '" + option.getKey() + "' in config of " + this.modId);
				}
			}
		}
	}

	/**
	 * Starts a new config.
	 *
	 * @param modId the id of your mod on the running loader. It is used to find the name, version and icon
	 *              of your mod, and as default for the file name and the translation id
	 */
	public static Builder builder(String modId) {
		return new Builder(modId);
	}

	// ---------------------------------------------------------------- info

	/** The mod id given to {@link #builder(String)}. */
	public String getModId() {
		return this.modId;
	}

	/** Used in translation keys: {@code config.<translation id>.<option key>}. Defaults to the mod id. */
	public String getTranslationId() {
		return this.translationId;
	}

	/** The file the config is saved in: {@code config/<file name>.json}. */
	public Path getFile() {
		return this.file;
	}

	/** Mod name from the mod metadata (fabric.mod.json / mods.toml), unless overridden in the builder. */
	public Component getDisplayName() {
		if (this.customName != null) {
			return this.customName;
		}
		return Component.literal(this.modInfo().map(ModInfo::name).orElse(this.modId));
	}

	/** Version of the mod from the loader's metadata, empty when it is unknown. */
	public String getVersion() {
		return this.modInfo().map(ModInfo::version).orElse("");
	}

	/** The icon set with {@link Builder#icon}, or null to use the icon from the mod metadata. */
	public @Nullable ResourceLocation getCustomIcon() {
		return this.customIcon;
	}

	/** Name, version and icon of the mod from the loader (fabric.mod.json / mods.toml). */
	public Optional<ModInfo> modInfo() {
		return Platform.mod(this.modId);
	}

	/** The categories (tabs) in display order. */
	public List<ConfigCategory> getCategories() {
		return this.categories;
	}

	/** Every option of this config, including the ones inside groups. */
	public java.util.Collection<Option<?>> getOptions() {
		return Collections.unmodifiableCollection(this.optionsByKey.values());
	}

	/** Finds an option by its key, or null. */
	public @Nullable Option<?> getOption(String key) {
		return this.optionsByKey.get(key);
	}

	// ---------------------------------------------------------------- editing (used by the screen)

	/** Whether the config screen has unsaved changes for this config. */
	public boolean hasChanges() {
		for (Option<?> option : this.optionsByKey.values()) {
			if (option.isDirty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Applies all pending values and writes the file.
	 *
	 * @return true when a changed option requires a restart
	 */
	public boolean commit() {
		boolean restart = false;
		boolean changed = false;
		for (Option<?> option : this.optionsByKey.values()) {
			boolean dirty = option.isDirty();
			if (option.commit()) {
				changed = true;
				restart |= dirty && option.requiresRestart();
			}
		}
		if (changed) {
			this.save();
		}
		return restart;
	}

	/** Throws away all unsaved changes of the config screen. */
	public void discard() {
		this.optionsByKey.values().forEach(Option::discard);
	}

	/** Sets every pending value back to its default (still needs to be saved). */
	public void resetPendingToDefaults() {
		this.optionsByKey.values().forEach(Option::resetPending);
	}

	// ---------------------------------------------------------------- file

	/**
	 * Reads the file and replaces the current values. {@link Builder#build()} already does this, call it
	 * yourself only to reload a file that was changed while the game is running.
	 * <p>
	 * Missing options are written back with their defaults. A file that can not be read is backed up as
	 * {@code <file name>.json.broken} and replaced by the defaults.
	 */
	public void load() {
		if (!Files.exists(this.file)) {
			this.save();
			return;
		}

		JsonObject json;
		try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			TabbyLib.LOGGER.error("Could not read config {}, a backup is created and defaults are used", this.file, e);
			this.backupBrokenFile();
			this.save();
			return;
		}

		boolean migrated = false;
		for (Predicate<JsonObject> migration : this.migrations) {
			try {
				migrated |= migration.test(json);
			} catch (Exception e) {
				TabbyLib.LOGGER.error("Config migration of {} failed", this.modId, e);
			}
		}
		if (migrated) {
			this.writeJson(json);
		}

		Set<String> missing = new HashSet<>();
		for (Option<?> option : this.optionsByKey.values()) {
			if (!this.loadOption(option, json)) {
				missing.add(option.getKey());
			}
		}

		// Writes new options (and removes nothing) so the file always shows every setting
		if (!missing.isEmpty()) {
			this.save();
		}
	}

	private <T> boolean loadOption(Option<T> option, JsonObject json) {
		if (option.toJson(option.getDefault()) == null) {
			return true;
		}
		JsonElement element = json.get(option.getKey());
		if (element == null || element.isJsonNull()) {
			return false;
		}
		try {
			option.loadValue(option.fromJson(element));
		} catch (Exception e) {
			TabbyLib.LOGGER.warn("Invalid value for '{}' in {}, using the default", option.getKey(), this.file);
			option.loadValue(option.getDefault());
		}
		return true;
	}

	/**
	 * Writes all values to the file and runs the {@link Builder#onSave} listeners.
	 * Keys in the file that this config does not know are kept.
	 */
	public void save() {
		JsonObject json = new JsonObject();
		// Keeps unknown keys of the old file, e.g. from a newer mod version
		if (Files.exists(this.file)) {
			try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
				JsonElement old = JsonParser.parseReader(reader);
				if (old.isJsonObject()) {
					old.getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
				}
			} catch (Exception ignored) {
				// The broken file is overwritten below
			}
		}
		for (Option<?> option : this.optionsByKey.values()) {
			writeOption(option, json);
		}

		this.writeJson(json);
		this.saveListeners.forEach(Runnable::run);
	}

	private void writeJson(JsonObject json) {
		try {
			Files.createDirectories(this.file.getParent());
			Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
			Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not save config {}", this.file, e);
		}
	}

	private static <T> void writeOption(Option<T> option, JsonObject json) {
		JsonElement element = option.toJson(option.get());
		if (element != null) {
			json.add(option.getKey(), element);
		}
	}

	private void backupBrokenFile() {
		try {
			Files.copy(this.file, this.file.resolveSibling(this.file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not back up broken config {}", this.file, e);
		}
	}

	// ---------------------------------------------------------------- builder

	/** Builder for a {@link TabbyConfig}, created with {@link TabbyConfig#builder(String)}. */
	public static final class Builder {
		private final String modId;
		private String translationId;
		private String fileName;
		private final List<ConfigCategory> categories = new ArrayList<>();
		private final List<Runnable> saveListeners = new ArrayList<>();
		private final List<Predicate<JsonObject>> migrations = new ArrayList<>();
		private @Nullable Component name;
		private @Nullable ResourceLocation icon;

		private Builder(String modId) {
			this.modId = modId;
			this.translationId = modId;
			this.fileName = modId;
		}

		/**
		 * Uses another id in translation keys. Handy when the mod id differs between loaders
		 * (e.g. "my-mod" on Fabric and "my_mod" on NeoForge) but the language files are shared.
		 */
		public Builder translationId(String translationId) {
			this.translationId = translationId;
			return this;
		}

		/** File name without ".json". Defaults to the mod id. Handy to keep an existing config file. */
		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		/** Overrides the name shown in the mod list. Defaults to the name in the mod metadata. */
		public Builder name(Component name) {
			this.name = name;
			return this;
		}

		/** Overrides the icon (a texture, e.g. {@code mymod:textures/gui/icon.png}). Defaults to the icon / logo in the mod metadata. */
		public Builder icon(ResourceLocation icon) {
			this.icon = icon;
			return this;
		}

		/** Adds a category you created yourself. */
		public Builder category(ConfigCategory category) {
			this.categories.add(category);
			return this;
		}

		/**
		 * Adds a new category (tab) and fills it:
		 * {@code .category("general", category -> category.add(SHOW_HUD, COLOR))}.
		 */
		public Builder category(String key, Consumer<ConfigCategory> content) {
			ConfigCategory category = new ConfigCategory(key);
			content.accept(category);
			return this.category(category);
		}

		/** Runs after the file was written. */
		public Builder onSave(Runnable listener) {
			this.saveListeners.add(listener);
			return this;
		}

		/**
		 * Rewrites an old config file before it is loaded, e.g. renamed or restructured keys.
		 * Edit the JSON object in place and return true when you changed something; the file is then
		 * written back so old keys you removed are gone for good. Runs in the order added.
		 */
		public Builder migration(Predicate<JsonObject> migration) {
			this.migrations.add(migration);
			return this;
		}

		/** Creates the config, registers it in TabbyLib and loads the file. */
		public TabbyConfig build() {
			if (this.categories.isEmpty()) {
				throw new IllegalStateException("Config of " + this.modId + " has no categories");
			}
			TabbyConfig config = new TabbyConfig(this);
			TabbyLibApi.register(config);
			config.load();
			return config;
		}
	}
}
