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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import java.util.function.Function;
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
	private final @Nullable Identifier customIcon;
	private final @Nullable Component description;
	private final @Nullable Function<Screen, Screen> customScreen;
	private final Map<String, Option<?>> optionsByKey = new LinkedHashMap<>();
	/** The options of each file. Categories with {@link ConfigCategory#file} get their own file. */
	private final Map<Path, List<Option<?>>> optionsByFile = new LinkedHashMap<>();

	private TabbyConfig(Builder builder) {
		this.modId = builder.modId;
		this.translationId = builder.translationId;
		this.file = Platform.configDir().resolve(builder.fileName + ".json");
		this.categories = List.copyOf(builder.categories);
		this.saveListeners = List.copyOf(builder.saveListeners);
		this.migrations = List.copyOf(builder.migrations);
		this.customName = builder.name;
		this.customIcon = builder.icon;
		this.description = builder.description;
		this.customScreen = builder.screen;

		for (ConfigCategory category : this.categories) {
			category.attach(this);
			Path categoryFile = category.getFileName() != null
				? Platform.configDir().resolve(category.getFileName() + ".json")
				: this.file;
			for (Option<?> option : category.getOptions()) {
				if (this.optionsByKey.put(option.getKey(), option) != null) {
					throw new IllegalStateException("Duplicate option key '" + option.getKey() + "' in config of " + this.modId);
				}
				this.optionsByFile.computeIfAbsent(categoryFile, path -> new ArrayList<>()).add(option);
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

	/** The main file of the config: {@code config/<file name>.json}. Categories can have own files, see {@link #getFiles()}. */
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
	public @Nullable Identifier getCustomIcon() {
		return this.customIcon;
	}

	/** The text set with {@link Builder#description}, or null. */
	public @Nullable Component getDescription() {
		return this.description;
	}

	/**
	 * The own config screen of the mod set with {@link Builder#screen}, or null when the TabbyLib screen shows
	 * the options. The function gets the parent screen.
	 */
	public @Nullable Function<Screen, Screen> getCustomScreen() {
		return this.customScreen;
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

	/** All files of this config: the main file and the own files of categories, if they have options. */
	public java.util.Collection<Path> getFiles() {
		return Collections.unmodifiableCollection(this.optionsByFile.keySet());
	}

	/**
	 * Reads the files and replaces the current values. {@link Builder#build()} already does this, call it
	 * yourself only to reload a file that was changed while the game is running.
	 * <p>
	 * Missing options are written back with their defaults. A file that can not be read is backed up as
	 * {@code <file name>.json.broken} and replaced by the defaults. When a category got its own file
	 * ({@link ConfigCategory#file}) that does not exist yet, its values are taken over from the main file.
	 */
	public void load() {
		for (Map.Entry<Path, List<Option<?>>> entry : this.optionsByFile.entrySet()) {
			this.loadFile(entry.getKey(), entry.getValue());
		}
	}

	private void loadFile(Path path, List<Option<?>> options) {
		if (!Files.exists(path)) {
			// Options that moved from the main file into an own file keep their values
			JsonObject old = !path.equals(this.file) && Files.exists(this.file) ? this.readJson(this.file) : null;
			if (old != null) {
				this.applyMigrations(old);
				for (Option<?> option : options) {
					this.loadOption(option, old, this.file);
				}
			}
			this.saveFile(path, options);
			return;
		}

		JsonObject json = this.readJson(path);
		if (json == null) {
			TabbyLib.LOGGER.error("Could not read config {}, a backup is created and defaults are used", path);
			this.backupBrokenFile(path);
			this.saveFile(path, options);
			return;
		}

		if (this.applyMigrations(json)) {
			this.writeJson(path, json);
		}

		Set<String> missing = new HashSet<>();
		for (Option<?> option : options) {
			if (!this.loadOption(option, json, path)) {
				missing.add(option.getKey());
			}
		}

		// Writes new options (and removes nothing) so the file always shows every setting
		if (!missing.isEmpty()) {
			this.saveFile(path, options);
		}
	}

	/** The JSON object of a file, or null when it can not be read. */
	private @Nullable JsonObject readJson(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			TabbyLib.LOGGER.debug("Could not read {}", path, e);
			return null;
		}
	}

	/** Runs the migrations on a file, true when one of them changed something. */
	private boolean applyMigrations(JsonObject json) {
		boolean migrated = false;
		for (Predicate<JsonObject> migration : this.migrations) {
			try {
				migrated |= migration.test(json);
			} catch (Exception e) {
				TabbyLib.LOGGER.error("Config migration of {} failed", this.modId, e);
			}
		}
		return migrated;
	}

	private <T> boolean loadOption(Option<T> option, JsonObject json, Path path) {
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
			TabbyLib.LOGGER.warn("Invalid value for '{}' in {}, using the default", option.getKey(), path);
			option.loadValue(option.getDefault());
		}
		return true;
	}

	/**
	 * Writes all values to the files and runs the {@link Builder#onSave} listeners.
	 * Keys in the files that this config does not know are kept.
	 */
	public void save() {
		for (Map.Entry<Path, List<Option<?>>> entry : this.optionsByFile.entrySet()) {
			this.saveFile(entry.getKey(), entry.getValue());
		}
		this.saveListeners.forEach(Runnable::run);
	}

	private void saveFile(Path path, List<Option<?>> options) {
		JsonObject json = new JsonObject();
		// Keeps unknown keys of the old file, e.g. from a newer mod version
		if (Files.exists(path)) {
			JsonObject old = this.readJson(path);
			if (old != null) {
				old.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
			}
		}
		for (Option<?> option : options) {
			writeOption(option, json);
		}
		this.writeJson(path, json);
	}

	private void writeJson(Path path, JsonObject json) {
		try {
			Files.createDirectories(path.getParent());
			Path temp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not save config {}", path, e);
		}
	}

	private static <T> void writeOption(Option<T> option, JsonObject json) {
		JsonElement element = option.toJson(option.get());
		if (element != null) {
			json.add(option.getKey(), element);
		}
	}

	private void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not back up broken config {}", path, e);
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
		private @Nullable Identifier icon;
		private @Nullable Component description;
		private @Nullable Function<Screen, Screen> screen;

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
		public Builder icon(Identifier icon) {
			this.icon = icon;
			return this;
		}

		/** Adds a category you created yourself. */
		/** Text about the mod, shown in the TabbyLib screen above the button to an own config screen. */
		public Builder description(Component description) {
			this.description = description;
			return this;
		}

		/**
		 * Uses an own config screen instead of the TabbyLib option list. The TabbyLib screen then shows the
		 * {@link #description} and a button that opens it: {@code .screen(parent -> new MyConfigScreen(parent))}.
		 * <p>
		 * The options still have to be added to categories, they are saved, loaded and searched as usual.
		 * The own screen can show them with {@code OptionListWidget} and {@code OptionHost}.
		 */
		public Builder screen(Function<Screen, Screen> factory) {
			this.screen = factory;
			return this;
		}

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
