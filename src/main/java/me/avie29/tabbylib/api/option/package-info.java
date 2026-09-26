/**
 * The option types. Each one is created with its static {@code builder(key, defaultValue)} method.
 *
 * <table>
 *     <caption>Option types</caption>
 *     <tr><th>Class</th><th>Value</th><th>In the config screen</th></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.BooleanOption}</td><td>boolean</td><td>On / off button</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.IntOption}</td><td>int</td><td>Slider or text field</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.DoubleOption}</td><td>double</td><td>Slider or text field</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.StringOption}</td><td>String</td><td>Text field with optional validation</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.EnumOption}</td><td>any enum</td><td>Button that cycles through the values</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.ColorOption}</td><td>ARGB int</td><td>Hex field and color picker</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.KeyBindOption}</td><td>key</td><td>Key binding, synced with the vanilla controls</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.StringListOption}</td><td>List of String</td><td>List editor</td></tr>
 *     <tr><td>{@link me.avie29.tabbylib.api.option.HudPositionOption}</td><td>{@link me.avie29.tabbylib.api.HudPosition}</td><td>Drag and drop HUD editor</td></tr>
 * </table>
 *
 * <p>Settings every type has (dependencies, bindings to existing fields, change listeners, custom names ...) are
 * in {@link me.avie29.tabbylib.api.option.Option.Builder}.
 *
 * <h2>Saved and pending values</h2>
 * Every option has a saved value ({@link me.avie29.tabbylib.api.option.Option#get()}), which your mod uses, and a
 * pending value ({@link me.avie29.tabbylib.api.option.Option#getPending()}), which the config screen edits.
 * The pending value becomes the saved one when the player presses "Save". Use {@code getPending()} for previews
 * and conditions that should react while the screen is open.
 */
package me.avie29.tabbylib.api.option;
