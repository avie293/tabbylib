package cc.avie.tabbylib.neoforge;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TabbyLib.MOD_ID, dist = Dist.CLIENT)
public class TabbyLibNeoForge {
	public TabbyLibNeoForge(IEventBus modBus, ModContainer container) {
		TabbyLib.init();
		// The "Config" button of TabbyLib in the mod list opens the overview of all mods
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> TabbyLibApi.createScreen(parent));
		modBus.addListener(FMLLoadCompleteEvent.class, event -> registerConfigScreens());
		NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
			event.getDispatcher().register(TabbyLib.command(CommandSourceStack::sendFailure)));
	}

	/** Like the Mod Menu buttons on Fabric: the "Config" button of a mod opens its TabbyLib screen, unless the mod has its own. */
	private static void registerConfigScreens() {
		for (TabbyConfig config : TabbyLibApi.getConfigs()) {
			String modId = config.getModId();
			if (modId.equals(TabbyLib.MOD_ID)) {
				continue;
			}
			ModList.get().getModContainerById(modId)
				.filter(mod -> mod.getCustomExtension(IConfigScreenFactory.class).isEmpty())
				.ifPresent(mod -> mod.registerExtensionPoint(IConfigScreenFactory.class,
					(container, parent) -> TabbyLibApi.createScreen(parent, modId)));
		}
	}
}
