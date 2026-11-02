package cc.avie.tabbylib;

import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/** Shared startup of TabbyLib, called by the entry point of each loader. */
public class TabbyLib {
	public static final String MOD_ID = "tabbylib";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void init() {
		TabbyLibConfig.init();
	}

	/** The {@code /tabbylib [mod]} client command, the loaders register it with their own command source. */
	public static <S> LiteralArgumentBuilder<S> command(BiConsumer<S, Component> sendError) {
		return LiteralArgumentBuilder.<S>literal(MOD_ID)
			.executes(context -> {
				TabbyLibApi.openScreen(null);
				return 1;
			})
			.then(RequiredArgumentBuilder.<S, String>argument("mod", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
					TabbyLibApi.getConfigs().stream().map(TabbyConfig::getModId), builder))
				.executes(context -> {
					String modId = StringArgumentType.getString(context, "mod");
					if (TabbyLibApi.getConfig(modId) == null) {
						sendError.accept(context.getSource(), Component.translatable("tabbylib.command.unknown", modId));
						return 0;
					}
					TabbyLibApi.openScreen(modId);
					return 1;
				}));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
