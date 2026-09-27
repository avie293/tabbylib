package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.TabbyLib;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entry point. The options menu button is added by OptionsScreenMixin. */
@Mod(value = TabbyLib.MOD_ID, dist = Dist.CLIENT)
public class TabbyLibNeoForge {
	public TabbyLibNeoForge() {
		TabbyLib.initClient();
		NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
			event.getDispatcher().register(TabbyLib.<CommandSourceStack>command(CommandSourceStack::sendFailure)));
	}
}
