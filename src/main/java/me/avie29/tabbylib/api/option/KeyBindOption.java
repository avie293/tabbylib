package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Shows a vanilla {@link KeyMapping} in the config screen, so players can rebind it without
 * searching through the controls menu. The key itself is still stored in options.txt,
 * so it stays in sync with the vanilla controls screen.
 * <p>
 * Client only. Register the mapping with Fabric's {@code KeyMappingHelper} as usual.
 */
public class KeyBindOption extends Option<InputConstants.Key> {
	private final KeyMapping mapping;

	protected KeyBindOption(Builder builder) {
		super(builder);
		this.mapping = builder.mapping;
	}

	public static Builder builder(String key, KeyMapping mapping) {
		return new Builder(key, mapping);
	}

	public KeyMapping getMapping() {
		return this.mapping;
	}

	@Override
	public Component getName() {
		Component name = super.getName();
		// Falls back to the key mapping's own name when the config has no translation for it
		if (!TranslationHelper.exists(this.translationKey())) {
			return Component.translatable(this.mapping.getName());
		}
		return name;
	}

	@Override
	public String formatValue(InputConstants.Key value) {
		return value.getDisplayName().getString();
	}

	@Override
	protected boolean valueEquals(InputConstants.@Nullable Key a, InputConstants.@Nullable Key b) {
		return a == null || b == null ? a == b : a.getName().equals(b.getName());
	}

	@Override
	public @Nullable JsonElement toJson(InputConstants.Key value) {
		return null;
	}

	@Override
	public InputConstants.Key fromJson(JsonElement json) {
		return InputConstants.getKey(json.getAsString());
	}

	public static class Builder extends Option.Builder<InputConstants.Key, Builder> {
		private final KeyMapping mapping;

		protected Builder(String key, KeyMapping mapping) {
			super(key, mapping.getDefaultKey());
			this.mapping = mapping;
			this.binding(
				() -> InputConstants.getKey(mapping.saveString()),
				newKey -> {
					mapping.setKey(newKey);
					KeyMapping.resetMapping();
					Minecraft.getInstance().options.save();
				}
			);
		}

		@Override
		public KeyBindOption build() {
			return new KeyBindOption(this);
		}
	}
}
