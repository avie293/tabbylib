package me.avie29.tabbylib.mixin;

import me.avie29.tabbylib.client.OptionsButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adds the "Tabby Config" button as a full width row below the vanilla option buttons.
 * The button becomes part of the vanilla button grid, so it moves with it when the screen is re-arranged
 * (resizing, coming back from a sub screen). Works without MixinExtras, so every loader can use it.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	protected OptionsScreenMixin(Component title) {
		super(title);
	}

	@ModifyArg(
		method = "init",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;")
	)
	private LayoutElement tabbylib$addConfigButton(LayoutElement contents) {
		if (OptionsButton.enabled() && contents instanceof GridLayout grid) {
			int[] children = {0};
			grid.visitChildren(child -> children[0]++);
			// The vanilla grid has 2 columns, the new row spans both
			int nextRow = (children[0] + 1) / 2;
			grid.addChild(OptionsButton.create(this), nextRow, 0, 1, 2);
		}
		return contents;
	}
}
