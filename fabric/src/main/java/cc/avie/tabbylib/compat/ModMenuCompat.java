package cc.avie.tabbylib.compat;

import cc.avie.tabbylib.TabbyLib;
import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.HashMap;
import java.util.Map;

public class ModMenuCompat implements ModMenuApi {
	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
		for (TabbyConfig config : TabbyLibApi.getConfigs()) {
			String modId = config.getModId();
			if (!modId.equals(TabbyLib.MOD_ID)) {
				factories.put(modId, parent -> TabbyLibApi.createScreen(parent, modId));
			}
		}
		return factories;
	}
}
