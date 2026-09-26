package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.TabbyLib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entry point. TabbyLib only does something on the client. The options menu button is added by OptionsScreenMixin. */
@Mod(TabbyLib.MOD_ID)
public final class TabbyLibForge {
	public TabbyLibForge() {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			return;
		}
		TabbyLib.initClient();
		MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
			event.getDispatcher().register(TabbyLib.<CommandSourceStack>command(CommandSourceStack::sendFailure)));
	}
}
