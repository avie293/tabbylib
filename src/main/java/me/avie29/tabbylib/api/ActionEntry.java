package me.avie29.tabbylib.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A button that runs code when clicked, e.g. "Open website" or "Clear cache".
 *
 * @param name       text left of the button
 * @param buttonText text on the button
 * @param tooltip    optional hover text
 * @param action     runs on the client thread when clicked
 */
public record ActionEntry(Component name, Component buttonText, @Nullable Component tooltip, Runnable action) implements ConfigEntry {
	public static ActionEntry of(Component name, Component buttonText, Runnable action) {
		return new ActionEntry(name, buttonText, null, action);
	}
}
