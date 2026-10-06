# Changelog

## 1.1.0

### Added
- Own config screens: `TabbyConfig.Builder.screen(parent -> new MyScreen(parent))` lets a mod design its own
  settings window. The TabbyLib screen then shows the mod's description and an "Open settings" button instead of
  the option list. Saving, loading and search keep working as before.
- `TabbyConfig.Builder.description(text)`: text about the mod, shown above that button.
- `OptionHost`: interface for screens that show TabbyLib option rows. Own screens implement it and use
  `OptionListWidget` to get the same controls as the TabbyLib screen (sliders, color picker, key bindings, ...).
- `ConfigCategory.file("my-mod/hud")`: saves the options of a category in their own file
  (`config/my-mod/hud.json`). Values from the old file are taken over the first time. `TabbyConfig.getFiles()`
  lists all files of a config.
- `OptionListWidget.addEntries(entries, indented)` fills the list with the entries of a category or group,
  including collapsible groups, labels and action buttons.

### Changed
- `OptionListWidget` and the option controls work with any `OptionHost` instead of only the TabbyLib screen.
- `OptionListWidget.clip` is public, for cutting texts to a width with "..." in own screens.
- The maven repository moved to https://avie.cc/maven.

## 1.0.0
- First release: config screen with mod list, categories, groups, search, all option types (toggle, numbers,
  text, enum, color, key binding, string list, HUD position), JSON config files with migrations.
