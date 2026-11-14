# TabbyLib

Config library with an in-game settings screen for Avie's mods.

TabbyLib gives every mod that uses it the same settings screen and saves their settings as JSON files in `config/tabbylib`. It is **client-side only** and **does not change gameplay, worlds or servers**.

**Note:** This mod does not add any content. It is a library used by other mods.

Features include:

- One overview with a panel for every mod that uses TabbyLib, with a search for mods and for single settings.
- Toggles, sliders, number and text fields, choices, colors with a color picker, key binds and editable lists.
- A HUD editor to move HUD elements by drag and drop, with snapping to edges, the center and a grid.
- Changes are only applied on Save and can be discarded or reset to their defaults.

The overview opens through the **Tabby Config** button in the options menu or with `/tabbylib`. The settings of a single mod also open with `/tabbylib <mod>`, through Mod Menu on Fabric and through the mod list on NeoForge.

Bugs and crashes can be reported in the [issue tracker](https://github.com/avie293/avie-issues/issues) by selecting **TabbyLib** as the project.

## Documentation

### Setup

TabbyLib is published to the maven at `https://avie.cc/maven`.

```groovy
repositories {
    maven {
        name = 'Avie'
        url = 'https://avie.cc/maven'
        content {
            includeGroupAndSubgroups 'cc.avie'
        }
    }
}

dependencies {
    // Fabric
    implementation "cc.avie.tabbylib:tabbylib:${tabbylib_version}"
    // NeoForge
    implementation "cc.avie.tabbylib:tabbylib-neoforge:${tabbylib_version}"
    // Common code of a multiloader project
    compileOnly "cc.avie.tabbylib:tabbylib-common:${tabbylib_version}"
}
```

Declare `tabbylib` as a required dependency in `fabric.mod.json` and `neoforge.mods.toml`.

### Creating a config

Options are created with `Options` and kept in static fields. `TabbyConfig.builder` puts them into categories (the tabs of the screen), registers the config and loads the file. Call it once from the client entry point.

```java
public final class ExampleConfig {
    public static final BooleanOption SHOW_HUD = Options.toggle("showHud", true);
    public static final IntOption SCALE = Options.integer("scale", 100).slider(50, 200, 5).dependsOn(SHOW_HUD);
    public static final ColorOption TEXT_COLOR = Options.color("textColor", 0xFFFFFF);

    public static void init() {
        TabbyConfig.builder("examplemod")
            .category("general", category -> category
                .add(SHOW_HUD, SCALE)
                .section("appearance", section -> section.add(TEXT_COLOR)))
            .build();
    }
}
```

Read a value with `SHOW_HUD.get()`. The settings are saved to `config/tabbylib/examplemod.json`; a broken file is backed up as `.broken` and replaced with the defaults.

### Options

| Factory | Value | Control |
|---|---|---|
| `Options.toggle` | `boolean` | On / off button |
| `Options.integer`, `Options.decimal` | `int`, `double` | Text field, or a slider with `.slider(min, max, step)` |
| `Options.text` | `String` | Text field, with `.maxLength`, `.validator` and `.hint` |
| `Options.choice` | Enum constant | Button that cycles through the constants |
| `Options.color`, `Options.colorWithAlpha` | `0xRRGGBB`, `0xAARRGGBB` | Hex field and color picker |
| `Options.keyBind` | Vanilla `KeyMapping` | Key bind button |
| `Options.textList` | `List<String>` | List editor |
| `Options.hudPosition` | `HudPosition` | HUD editor |

Every option also has `.onChange(listener)`, `.bind(getter, setter)` to store the value in an existing field, `.enabledWhen(condition)`, `.dependsOn(toggle)`, `.visibleWhen(condition)`, `.requiresRestart()` and `.hidden()`.

Categories and sections can also contain text with `.label(text)` and buttons with `.action(name, buttonText, action)`. A category is saved to its own file with `.file(name)`.

### Translations

Names come from the lang file of the mod. `.name(...)` and `.tooltip(...)` set a fixed text instead, and `Builder#translationId` replaces the mod id in the keys.

```json
{
  "config.examplemod.category.general": "General",
  "config.examplemod.section.appearance": "Appearance",
  "config.examplemod.showHud": "Show HUD",
  "config.examplemod.showHud.tooltip": "Shows the HUD in the top left corner."
}
```

The tooltip key is optional. The constants of a choice use `config.examplemod.<option>.<constant in lower case>` and fall back to their name.

### HUD positions

A `HudPosition` is an anchor (`START`, `CENTER` or `END` on each axis) and an offset, so it stays in place when the window size or the GUI scale changes. The option needs a `HudPreview` that draws the element in the editor.

```java
public static final HudPositionOption POSITION = Options.hudPosition("position",
    HudPosition.of(HudPosition.START, HudPosition.START, 4, 4), ExampleHudPreview::new);

HudPosition position = POSITION.get();
int x = position.x(screenWidth, elementWidth);
int y = position.y(screenHeight, elementHeight);
```

### More

- `Builder#name`, `#icon` and `#description` change the panel of the mod in the overview, which otherwise shows the name and icon from the mod metadata.
- `Builder#screen` opens an own screen instead of the option list.
- `Builder#onSave` runs after the files were written, `Builder#migration` rewrites an old config file before it is loaded.
- `TabbyLibApi.createScreen(parent, modId)` and `TabbyLibApi.openScreen(modId)` open the settings of a mod from code.
