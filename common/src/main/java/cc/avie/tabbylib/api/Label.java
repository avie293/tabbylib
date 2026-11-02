package cc.avie.tabbylib.api;

import net.minecraft.network.chat.Component;

/** A word wrapped line of text between options. */
public record Label(Component text) implements ConfigEntry {
}
