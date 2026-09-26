package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.TabbyLib;
import me.avie29.tabbylib.client.OptionsButton;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entry point. */
@Mod(value = TabbyLib.MOD_ID, dist = Dist.CLIENT)
public class TabbyLibNeoForge {
	public TabbyLibNeoForge() {
		TabbyLib.initClient();
		NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
			event.getDispatcher().register(TabbyLib.<CommandSourceStack>command(CommandSourceStack::sendFailure)));
		NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
			if (event.getScreen() instanceof OptionsScreen screen) {
				OptionsButton.addBelowGrid(screen, event.getListenersList(), event::addListener);
			}
		});
	}
}
