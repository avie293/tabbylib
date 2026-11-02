package cc.avie.tabbylib.api;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.option.Option;
import cc.avie.tabbylib.platform.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** The config of one mod: its categories and options, saved as JSON in {@code config/tabbylib/<file name>.json}. */
public final class TabbyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final String modId;
	private final String translationId;
	private final Path file;
	private final List<Category> categories;
	private final List<Runnable> saveListeners;
	private final List<Predicate<JsonObject>> migrations;
	private final @Nullable Component customName;
	private final @Nullable Identifier customIcon;
	private final @Nullable Component description;
	private final @Nullable Function<Screen, Screen> customScreen;
	private final Map<String, Option<?, ?>> optionsByKey = new LinkedHashMap<>();
	/** The options of each file. */
	private final Map<Path, List<Option<?, ?>>> optionsByFile = new LinkedHashMap<>();
	/** Read from the mod metadata on first use, the metadata does not change while the game runs. */
	private @Nullable Component displayName;
	private @Nullable String version;

	private TabbyConfig(Builder builder) {
		this.modId = builder.modId;
		this.translationId = builder.translationId;
		this.file = configDir().resolve(builder.fileName + ".json");
		this.categories = List.copyOf(builder.categories);
		this.saveListeners = List.copyOf(builder.saveListeners);
		this.migrations = List.copyOf(builder.migrations);
		this.customName = builder.name;
		this.customIcon = builder.icon;
		this.description = builder.description;
		this.customScreen = builder.screen;

		for (Category category : this.categories) {
			category.attach(this);
			Path categoryFile = category.getFileName() != null ? configDir().resolve(category.getFileName() + ".json") : this.file;
			for (Option<?, ?> option : category.getOptions()) {
				if (this.optionsByKey.put(option.getKey(), option) != null) {
					throw new IllegalStateException("Duplicate option key '" + option.getKey() + "' in the config of " + this.modId);
				}
				this.optionsByFile.computeIfAbsent(categoryFile, path -> new ArrayList<>()).add(option);
			}
		}
	}

	/** Starts a new config. */
	public static Builder builder(String modId) {
		return new Builder(modId);
	}

	/** {@code config/tabbylib}: TabbyLib keeps the files of all mods in its own folder. */
	public static Path configDir() {
		return Platform.get().configDir().resolve(TabbyLib.MOD_ID);
	}

	public String getModId() {
		return this.modId;
	}

	/** Used in translation keys: {@code config.<translation id>.<option key>}. */
	public String getTranslationId() {
		return this.translationId;
	}

	/** The main file. */
	public Path getFile() {
		return this.file;
	}

	/** Name from fabric.mod.json or neoforge.mods.toml, unless set with {@link Builder#name}. */
	public Component getDisplayName() {
		if (this.customName != null) {
			return this.customName;
		}
		if (this.displayName == null) {
			this.displayName = Component.literal(Platform.get().modName(this.modId).orElse(this.modId));
		}
		return this.displayName;
	}

	/** Version from fabric.mod.json or neoforge.mods.toml, empty when the mod is unknown. */
	public String getVersion() {
		if (this.version == null) {
			this.version = Platform.get().modVersion(this.modId).orElse("");
		}
		return this.version;
	}

	/** The icon set with {@link Builder#icon}, or null to use the icon from the mod metadata. */
	public @Nullable Identifier getCustomIcon() {
		return this.customIcon;
	}

	/** The text set with {@link Builder#description}, shown when hovering the panel. */
	public @Nullable Component getDescription() {
		return this.description;
	}

	/** The own screen set with {@link Builder#screen}, or null. */
	public @Nullable Function<Screen, Screen> getCustomScreen() {
		return this.customScreen;
	}

	/**
	 * The Fabric mod of this config.
	 *
	 * @deprecated Only works on Fabric and fails on other loaders, use {@link #getDisplayName()} and {@link #getVersion()}.
	 */
	@Deprecated
	public Optional<ModContainer> modContainer() {
		return FabricLoader.getInstance().getModContainer(this.modId);
	}

	/** The categories (tabs) in display order. */
	public List<Category> getCategories() {
		return this.categories;
	}

	/** Every option, including the ones inside sections. */
	public Collection<Option<?, ?>> getOptions() {
		return Collections.unmodifiableCollection(this.optionsByKey.values());
	}

	public @Nullable Option<?, ?> getOption(String key) {
		return this.optionsByKey.get(key);
	}

	/** Whether the config screen has unsaved changes. */
	public boolean hasChanges() {
		for (Option<?, ?> option : this.optionsByKey.values()) {
			if (option.isDirty()) {
				return true;
			}
		}
		return false;
	}

	/** Applies all pending values and writes the files. */
	public boolean commit() {
		boolean restart = false;
		boolean changed = false;
		for (Option<?, ?> option : this.optionsByKey.values()) {
			boolean dirty = option.isDirty();
			if (option.commit()) {
				changed = true;
				restart |= dirty && option.isRestartRequired();
			}
		}
		if (changed) {
			this.save();
		}
		return restart;
	}

	/** Throws away all unsaved changes. */
	public void discard() {
		this.optionsByKey.values().forEach(Option::discard);
	}

	/** Sets every pending value back to its default (still needs to be saved). */
	public void resetPendingToDefaults() {
		this.optionsByKey.values().forEach(Option::resetPending);
	}

	/** The main file and the own files of categories that have options. */
	public Collection<Path> getFiles() {
		return Collections.unmodifiableCollection(this.optionsByFile.keySet());
	}

	/** Reads the files and replaces the saved values. */
	public void load() {
		moveFromOldLocation(this.file);
		this.optionsByFile.keySet().forEach(TabbyConfig::moveFromOldLocation);
		for (Map.Entry<Path, List<Option<?, ?>>> entry : this.optionsByFile.entrySet()) {
			this.loadFile(entry.getKey(), entry.getValue());
		}
	}

	/** Files used to be directly in {@code config/}. */
	private static void moveFromOldLocation(Path path) {
		Path old = Platform.get().configDir().resolve(configDir().relativize(path));
		if (Files.exists(path) || !Files.exists(old)) {
			return;
		}
		try {
			Files.createDirectories(path.getParent());
			Files.move(old, path);
			TabbyLib.LOGGER.info("Moved config {} to {}", old, path);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not move config {} to {}", old, path, e);
		}
	}

	private void loadFile(Path path, List<Option<?, ?>> options) {
		if (!Files.exists(path)) {
			JsonObject old = !path.equals(this.file) && Files.exists(this.file) ? this.readJson(this.file) : null;
			if (old != null) {
				this.applyMigrations(old);
				for (Option<?, ?> option : options) {
					loadOption(option, old, this.file);
				}
			}
			this.saveFile(path, options);
			return;
		}

		JsonObject json = this.readJson(path);
		if (json == null) {
			TabbyLib.LOGGER.error("Could not read config {}, a backup is created and defaults are used", path);
			backupBrokenFile(path);
			this.saveFile(path, options);
			return;
		}

		boolean migrated = this.applyMigrations(json);
		boolean missing = false;
		for (Option<?, ?> option : options) {
			missing |= !loadOption(option, json, path);
		}
		// Written once: with the missing values added, or as migrated
		if (missing) {
			this.saveFile(path, options, json);
		} else if (migrated) {
			writeJson(path, json);
		}
	}

	private @Nullable JsonObject readJson(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			TabbyLib.LOGGER.debug("Could not read {}", path, e);
			return null;
		}
	}

	/** Runs the migrations, true when one of them changed something. */
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

	/** False when the file has no value for the option. */
	private static <T> boolean loadOption(Option<T, ?> option, JsonObject json, Path path) {
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

	/** Writes all saved values to the files and runs the {@link Builder#onSave} listeners. */
	public void save() {
		for (Map.Entry<Path, List<Option<?, ?>>> entry : this.optionsByFile.entrySet()) {
			this.saveFile(entry.getKey(), entry.getValue());
		}
		this.saveListeners.forEach(Runnable::run);
	}

	private void saveFile(Path path, List<Option<?, ?>> options) {
		this.saveFile(path, options, Files.exists(path) ? this.readJson(path) : null);
	}

	/** Keeps the unknown keys of the old content and only writes when the content changed. */
	private void saveFile(Path path, List<Option<?, ?>> options, @Nullable JsonObject old) {
		JsonObject json = new JsonObject();
		if (old != null) {
			old.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
		}
		for (Option<?, ?> option : options) {
			writeOption(option, json);
		}
		if (!json.equals(old)) {
			writeJson(path, json);
		}
	}

	private static <T> void writeOption(Option<T, ?> option, JsonObject json) {
		JsonElement element = option.toJson(option.get());
		if (element != null) {
			json.add(option.getKey(), element);
		}
	}

	private static void writeJson(Path path, JsonObject json) {
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

	private static void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			TabbyLib.LOGGER.error("Could not back up broken config {}", path, e);
		}
	}

	/** Created with {@link TabbyConfig#builder(String)}. */
	public static final class Builder {
		private final String modId;
		private String translationId;
		private String fileName;
		private final List<Category> categories = new ArrayList<>();
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

		/** Uses another id in translation keys than the mod id. */
		public Builder translationId(String translationId) {
			this.translationId = translationId;
			return this;
		}

		/** File name without ".json". */
		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		/** Overrides the name on the panel. */
		public Builder name(Component name) {
			this.name = name;
			return this;
		}

		/** Overrides the logo on the panel. */
		public Builder icon(Identifier icon) {
			this.icon = icon;
			return this;
		}

		/** Text shown when hovering the panel of the mod. */
		public Builder description(Component description) {
			this.description = description;
			return this;
		}

		/** Opens an own config screen when the panel is clicked, instead of the TabbyLib option list: {@code .screen(parent -> new MyConfigScreen(parent))}. */
		public Builder screen(Function<Screen, Screen> factory) {
			this.screen = factory;
			return this;
		}

		/** Adds a category (tab) and fills it: {@code .category("general", category -> category.add(SHOW_HUD))}. */
		public Builder category(String key, Consumer<Category> content) {
			Category category = new Category(key);
			content.accept(category);
			this.categories.add(category);
			return this;
		}

		/** Runs after the files were written. */
		public Builder onSave(Runnable listener) {
			this.saveListeners.add(listener);
			return this;
		}

		/** Rewrites an old config file before it is loaded. */
		public Builder migration(Predicate<JsonObject> migration) {
			this.migrations.add(migration);
			return this;
		}

		/** Creates the config, registers it in TabbyLib and loads the files. */
		public TabbyConfig build() {
			if (this.categories.isEmpty()) {
				throw new IllegalStateException("The config of " + this.modId + " has no categories");
			}
			TabbyConfig config = new TabbyConfig(this);
			TabbyLibApi.register(config);
			config.load();
			return config;
		}
	}
}
