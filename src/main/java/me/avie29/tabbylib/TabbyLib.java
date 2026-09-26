package me.avie29.tabbylib;

import me.avie29.tabbylib.compat.McCompat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.api.TabbyLibApi;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/** Loader independent part of TabbyLib. The loader entry points live in the platform package. */
public final class TabbyLib {
	public static final String MOD_ID = "tabbylib";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private TabbyLib() {
	}

	/** Called once by the loader entry point on the client. */
	public static void initClient() {
		TabbyLibConfig.init();
	}

	/**
	 * The /tabbylib command. Brigadier is the same on every loader, only the command source differs.
	 * <p>
	 * /tabbylib            opens the config screen
	 * /tabbylib &lt;mod id&gt;   opens it with that mod selected
	 */
	public static <S> LiteralArgumentBuilder<S> command(BiConsumer<S, Component> sendError) {
		return LiteralArgumentBuilder.<S>literal("tabbylib")
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

	public static ResourceLocation id(String path) {
		return McCompat.id(MOD_ID, path);
	}
}
