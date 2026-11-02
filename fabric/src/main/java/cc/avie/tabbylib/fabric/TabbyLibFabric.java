package cc.avie.tabbylib.fabric;

import cc.avie.tabbylib.TabbyLib;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class TabbyLibFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TabbyLib.init();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(TabbyLib.command(FabricClientCommandSource::sendError)));
	}
}
