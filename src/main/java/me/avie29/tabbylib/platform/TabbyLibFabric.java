package me.avie29.tabbylib.platform;

import me.avie29.tabbylib.TabbyLib;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** Fabric entry point. The options menu button is added by OptionsScreenMixin. */
public class TabbyLibFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TabbyLib.initClient();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(TabbyLib.<FabricClientCommandSource>command(FabricClientCommandSource::sendError)));
	}
}
