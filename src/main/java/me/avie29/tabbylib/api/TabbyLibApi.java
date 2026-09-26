package me.avie29.tabbylib.api;

import me.avie29.tabbylib.TabbyLib;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Entry point for mods: registered configs and opening the config screen. */
public final class TabbyLibApi {
	private static final Map<String, TabbyConfig> CONFIGS = new LinkedHashMap<>();

	private TabbyLibApi() {
	}

	/** Called by {@link TabbyConfig.Builder#build()}. You normally don't need to call this yourself. */
	public static synchronized void register(TabbyConfig config) {
		if (CONFIGS.containsKey(config.getModId())) {
			throw new IllegalStateException("A TabbyLib config for " + config.getModId() + " is already registered");
		}
		CONFIGS.put(config.getModId(), config);
		me.avie29.tabbylib.platform.Platform.onConfigRegistered(config);
		TabbyLib.LOGGER.info("Registered config for {}", config.getModId());
	}

	/** All registered configs. TabbyLib's own config comes first, the rest sorted by name. */
	public static synchronized List<TabbyConfig> getConfigs() {
		List<TabbyConfig> list = new ArrayList<>(CONFIGS.values());
		list.sort(Comparator
			.comparing((TabbyConfig config) -> !config.getModId().equals(TabbyLib.MOD_ID))
			.thenComparing(config -> config.getDisplayName().getString().toLowerCase()));
		return Collections.unmodifiableList(list);
	}

	public static synchronized @Nullable TabbyConfig getConfig(String modId) {
		return CONFIGS.get(modId);
	}

	/**
	 * Creates the TabbyLib config screen with the given mod selected.
	 * Use this for Mod Menu integration: {@code parent -> TabbyLibApi.createScreen(parent, MOD_ID)}.
	 */
	public static Screen createScreen(@Nullable Screen parent, @Nullable String modId) {
		return new me.avie29.tabbylib.client.gui.TabbyConfigScreen(parent, modId);
	}

	/**
	 * True while the HUD position editor is open. Check this in your HUD renderer and skip drawing,
	 * otherwise the element shows up twice (at the saved and at the edited position).
	 */
	public static boolean isHudEditorOpen() {
		return me.avie29.tabbylib.client.gui.HudEditorScreen.isOpen();
	}

	/** Opens the HUD position editor of an option directly, without the config screen. Changes are saved on "Done". */
	public static void openHudEditor(me.avie29.tabbylib.api.option.HudPositionOption option) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> McCompat.setScreen(new me.avie29.tabbylib.client.gui.HudEditorScreen(McCompat.currentScreen(), option, true)));
	}

	/** Opens the config screen with the given mod selected, e.g. from a command or key binding. */
	public static void openScreen(@Nullable String modId) {
		Minecraft minecraft = Minecraft.getInstance();
		// Commands run while the chat screen is still open, so the screen is set on the next tick
		minecraft.execute(() -> McCompat.setScreen(createScreen(McCompat.currentScreen(), modId)));
	}
}
