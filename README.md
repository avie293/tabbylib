# TabbyLib
Config Library for Avies Mods

Adds a "Tabby Config..." button to the options menu (also `/tabbylib [mod id]`). The screen shows every mod that
uses TabbyLib on the left and its settings on the right. Settings are only applied when you press Save.

## Features
- Option types: toggle, whole number / decimal (slider or text field), text, enum, color (hex field + color picker with
  alpha), key binding (synced with the vanilla controls), string list, HUD position (drag and drop editor with snapping)
- Categories (tabs), collapsible groups, text labels and action buttons
- Search (Ctrl+F) across all categories, Ctrl+S saves
- Save / discard / reset per option or per mod, unsaved changes are marked and confirmed on close
- Tooltips with default value, "requires restart" hint, options that depend on other options (`dependsOn`,
  `enabledWhen`, `visibleWhen`)
- Automatic translation keys, JSON config file with atomic saving, backup of broken files, migrations for old files
- Options can store their own value or bind to your existing fields (`binding(getter, setter)`)
- Mod Menu: `parent -> TabbyLibApi.createScreen(parent, MOD_ID)`
- NeoForge / Forge: the "Config" button in the loader's mod list opens the TabbyLib screen automatically
- Own config screens: keep TabbyLib for saving and the option rows, but design the window yourself

## Using it in a mod
Full guide: https://avie.cc/wiki/tabbylib/

`build.gradle`:
```groovy
repositories {
	maven { url = "https://avie.cc/maven" }
}
dependencies {
	// Fabric (1.21.1: modImplementation)
	implementation "me.avie29.tabbylib:tabbylib:1.1.0+26.2"
	// NeoForge
	implementation "me.avie29.tabbylib:tabbylib-neoforge:1.1.0+26.2"
	// Forge (1.20.1: implementation fg.deobf("..."))
	implementation "me.avie29.tabbylib:tabbylib-forge:1.1.0+26.2"
}
```
Add TabbyLib as dependency: `"tabbylib": ">=1.1.0"` in `fabric.mod.json` (`depends`), or a `[[dependencies.<modid>]]`
entry with `modId="tabbylib"` in `neoforge.mods.toml` / `mods.toml`.

If your mod id differs between loaders (NeoForge and Forge do not allow `-`), call `.translationId("my-mod")` on the
config builder, so all loaders use the same language files.

```java
public final class MyConfig {
	public static final BooleanOption SHOW_HUD = BooleanOption.builder("showHud", true).build();
	public static final ColorOption COLOR = ColorOption.builder("color", 0xFFFFFFFF).alpha().dependsOn(SHOW_HUD).build();
	public static final IntOption SIZE = IntOption.builder("size", 10).slider(1, 50, 1).build();

	public static TabbyConfig CONFIG;

	// Call in your client initializer
	public static void init() {
		CONFIG = TabbyConfig.builder("my-mod")
			.category("general", category -> category
				.add(SHOW_HUD)
				.group("look", group -> group.add(COLOR, SIZE)))
			.build(); // registers and loads config/my-mod.json
	}
}
```
Read values with `SHOW_HUD.get()`. Change them in code with `set(...)` followed by `CONFIG.save()`.

Translations (`assets/<modid>/lang/en_us.json`):
```
config.<modid>.<option key>            name
config.<modid>.<option key>.tooltip    tooltip (optional)
config.<modid>.<option key>.<enum>     enum value, lower case (optional)
config.<modid>.category.<key>          tab name
config.<modid>.group.<key>             group name
```

### HUD elements
`HudPositionOption.builder("position", HudPosition.of(HudPosition.CENTER, HudPosition.END, 0, -50), MyHud::preview)`
stores a position that stays at its anchor when the window size changes. Draw with
`position.x(screenWidth, width)` / `position.y(screenHeight, height)`, skip drawing while
`TabbyLibApi.isHudEditorOpen()` and open the editor directly with `TabbyLibApi.openHudEditor(option)`.

### Own config screen
Mods that want their own settings window still register a normal config, so saving, loading and search keep
working, and add `.description(text)` and `.screen(parent -> new MyScreen(parent))` to the builder. The TabbyLib
screen then shows the description and an "Open settings" button instead of the options.

The own screen can show TabbyLib option rows: implement `OptionHost`, create an `OptionListWidget` and fill it with
`addEntries(category.getEntries(), false)`. Changes are pending like in the TabbyLib screen, call `config.commit()`
to save them and `config.discard()` to throw them away. Forward key presses to the capturing key binding control,
like `TabbyConfigScreen` does.

### Several files
`.category("hud", category -> category.file("my-mod/hud").add(...))` saves a category in its own file
(`config/my-mod/hud.json`) instead of the file of the config. The first time, the values are taken over from the
old file, so splitting a config later keeps the settings of the players.

Changes of every version are listed in `CHANGELOG.md`.

# License
This mod is licensed under Creative Commons Attribution-NonCommercial 4.0 International
