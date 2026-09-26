/**
 * The TabbyLib API. Everything a mod needs is in this package and in {@link me.avie29.tabbylib.api.option}.
 * All other packages of TabbyLib are internal and may change in any version.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * public final class MyConfig {
 *     public static final BooleanOption SHOW_HUD = BooleanOption.builder("showHud", true).build();
 *     public static final IntOption SIZE = IntOption.builder("size", 10).slider(1, 50, 1).build();
 *
 *     private static TabbyConfig config;
 *
 *     // Call once on the client: Fabric client initializer, NeoForge / Forge mod constructor
 *     public static void init() {
 *         config = TabbyConfig.builder("my-mod")
 *             .category("general", category -> category.add(SHOW_HUD, SIZE))
 *             .build(); // registers the config and loads config/my-mod.json
 *     }
 * }
 * }</pre>
 * Read values with {@code SHOW_HUD.get()}. The config then shows up in the "Tabby Config" screen
 * (options menu, {@code /tabbylib}, and on NeoForge / Forge the "Config" button of the mod list).
 *
 * <h2>Main classes</h2>
 * <ul>
 *     <li>{@link me.avie29.tabbylib.api.TabbyConfig} - the config of one mod: categories, file, saving</li>
 *     <li>{@link me.avie29.tabbylib.api.ConfigCategory} and {@link me.avie29.tabbylib.api.OptionGroup} - tabs and
 *     collapsible sections</li>
 *     <li>{@link me.avie29.tabbylib.api.LabelEntry} and {@link me.avie29.tabbylib.api.ActionEntry} - text lines and
 *     buttons between the options</li>
 *     <li>{@link me.avie29.tabbylib.api.HudPosition} and {@link me.avie29.tabbylib.api.HudPreview} - positions of HUD
 *     elements, edited with drag and drop</li>
 *     <li>{@link me.avie29.tabbylib.api.TabbyLibApi} - opening the screen, the HUD editor and finding configs</li>
 * </ul>
 *
 * <h2>Translations</h2>
 * Names are looked up in your language files: {@code config.<translation id>.<option key>} for options (plus
 * {@code .tooltip} for the tooltip), {@code config.<translation id>.category.<key>} and
 * {@code config.<translation id>.group.<key>}. The translation id is the mod id unless you set
 * {@link me.avie29.tabbylib.api.TabbyConfig.Builder#translationId(String)}.
 *
 * <p>Full guide: <a href="https://avie29.me/tabbylib/wiki/">avie29.me/tabbylib/wiki</a>
 */
package me.avie29.tabbylib.api;
