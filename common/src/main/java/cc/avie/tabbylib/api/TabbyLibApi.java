package cc.avie.tabbylib.api;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.option.HudPositionOption;
import cc.avie.tabbylib.client.gui.HudEditorScreen;
import cc.avie.tabbylib.client.gui.ModConfigScreen;
import cc.avie.tabbylib.client.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Registered configs and opening the TabbyLib screens. */
public final class TabbyLibApi {
	private static final Map<String, TabbyConfig> CONFIGS = new LinkedHashMap<>();

	private TabbyLibApi() {
	}

	/** Called by {@link TabbyConfig.Builder#build()}. */
	static synchronized void register(TabbyConfig config) {
		if (CONFIGS.containsKey(config.getModId())) {
			throw new IllegalStateException("A TabbyLib config for " + config.getModId() + " is already registered");
		}
		CONFIGS.put(config.getModId(), config);
		TabbyLib.LOGGER.info("Registered config for {}", config.getModId());
	}

	/** All registered configs. */
	public static synchronized List<TabbyConfig> getConfigs() {
		List<TabbyConfig> list = new ArrayList<>(CONFIGS.values());
		list.sort(Comparator
			.comparing((TabbyConfig config) -> !config.getModId().equals(TabbyLib.MOD_ID))
			.thenComparing(config -> config.getDisplayName().getString().toLowerCase()));
		return Collections.unmodifiableList(list);
	}

	/** The config of a mod, or null. */
	public static synchronized @Nullable TabbyConfig getConfig(String modId) {
		return CONFIGS.get(modId);
	}

	/** The overview with the panels of all mods. */
	public static Screen createScreen(@Nullable Screen parent) {
		return new ModsScreen(parent);
	}

	/** The settings of one mod, closing them returns to the parent. */
	public static Screen createScreen(@Nullable Screen parent, String modId) {
		TabbyConfig config = getConfig(modId);
		if (config == null) {
			return new ModsScreen(parent);
		}
		Function<Screen, Screen> customScreen = config.getCustomScreen();
		return customScreen != null ? customScreen.apply(parent) : new ModConfigScreen(parent, config);
	}

	/** Opens the overview, or the settings of a mod when the id is not null. */
	public static void openScreen(@Nullable String modId) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			Screen current = minecraft.gui.screen();
			minecraft.gui.setScreen(modId != null ? createScreen(current, modId) : createScreen(current));
		});
	}

	/** True while the HUD editor is open. */
	public static boolean isHudEditorOpen() {
		return HudEditorScreen.isOpen();
	}

	/** Opens the HUD editor of an option without the config screen. */
	public static void openHudEditor(HudPositionOption option) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.gui.setScreen(new HudEditorScreen(minecraft.gui.screen(), option, true)));
	}
}
