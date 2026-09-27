/**
 * Everything that depends on the mod loader (config folder, mod metadata, entry points).
 * Every loader has its own {@code Platform} class with the same methods, the rest of TabbyLib only calls those.
 * <p>
 * Internal, not part of the API: may change in any version. Mods use {@link me.avie29.tabbylib.api} instead.
 */
package me.avie29.tabbylib.platform;
