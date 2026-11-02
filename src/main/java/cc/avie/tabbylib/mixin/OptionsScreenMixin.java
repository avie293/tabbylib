package cc.avie.tabbylib.mixin;

import cc.avie.tabbylib.client.OptionsButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
			int nextRow = (children[0] + 1) / 2;
			grid.addChild(OptionsButton.create(this), nextRow, 0, 1, 2);
		}
		return contents;
	}
}
