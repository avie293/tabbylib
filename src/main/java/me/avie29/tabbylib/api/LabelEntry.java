package me.avie29.tabbylib.api;

import net.minecraft.network.chat.Component;

/** A (word wrapped) line of text between options. */
public record LabelEntry(Component text) implements ConfigEntry {
}
