package cc.avie.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** A vanilla {@link KeyMapping} in the config screen, created with {@link Options#keyBind}. */
public class KeyBindOption extends Option<InputConstants.Key, KeyBindOption> {
	private final KeyMapping mapping;

	protected KeyBindOption(String key, KeyMapping mapping) {
		super(key, mapping.getDefaultKey());
		this.mapping = mapping;
		this.bind(
			() -> InputConstants.getKey(mapping.saveString()),
			newKey -> {
				mapping.setKey(newKey);
				KeyMapping.resetMapping();
				Minecraft.getInstance().options.save();
			}
		);
	}

	/** The key mapping this option edits. */
	public KeyMapping getMapping() {
		return this.mapping;
	}

	@Override
	public Component getName() {
		if (!TranslationHelper.exists(this.translationKey())) {
			return Component.translatable(this.mapping.getName());
		}
		return super.getName();
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
}
