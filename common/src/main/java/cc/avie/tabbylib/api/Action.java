package cc.avie.tabbylib.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** A button between the options that runs code. */
public record Action(Component name, Component buttonText, @Nullable Component tooltip, Runnable action) implements ConfigEntry {
	/** An action without tooltip. */
	public static Action of(Component name, Component buttonText, Runnable action) {
		return new Action(name, buttonText, null, action);
	}
}
