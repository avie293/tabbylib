package cc.avie.tabbylib.client;

import cc.avie.tabbylib.TabbyLibConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OptionsButton {
	public static final int WIDTH = Button.DEFAULT_WIDTH * 2 + 8;

	private OptionsButton() {
	}

	public static boolean enabled() {
		return TabbyLibConfig.OPTIONS_BUTTON.get() && !TabbyLibApi.getConfigs().isEmpty();
	}

	public static Button create(Screen parent) {
		return Button.builder(Component.translatable("tabbylib.options_button"),
				button -> Minecraft.getInstance().gui.setScreen(TabbyLibApi.createScreen(parent)))
			.width(WIDTH)
			.build();
	}
}
