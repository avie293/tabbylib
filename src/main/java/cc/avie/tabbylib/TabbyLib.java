package cc.avie.tabbylib;

import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class TabbyLib implements ClientModInitializer {
	public static final String MOD_ID = "tabbylib";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		TabbyLibConfig.init();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal(MOD_ID)
			.executes(context -> {
				TabbyLibApi.openScreen(null);
				return 1;
			})
			.then(argument("mod", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
					TabbyLibApi.getConfigs().stream().map(TabbyConfig::getModId), builder))
				.executes(context -> {
					String modId = StringArgumentType.getString(context, "mod");
					if (TabbyLibApi.getConfig(modId) == null) {
						context.getSource().sendError(Component.translatable("tabbylib.command.unknown", modId));
						return 0;
					}
					TabbyLibApi.openScreen(modId);
					return 1;
				}))));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
