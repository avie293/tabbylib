package cc.avie.tabbylib.api.option;

import cc.avie.tabbylib.api.HudPosition;
import cc.avie.tabbylib.api.HudPreview;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.function.Supplier;

/** Position of a HUD element, created with {@link Options#hudPosition}. */
public class HudPositionOption extends Option<HudPosition, HudPositionOption> {
	private final Supplier<HudPreview> preview;

	protected HudPositionOption(String key, HudPosition defaultValue, Supplier<HudPreview> preview) {
		super(key, defaultValue);
		this.preview = preview;
	}

	/** Creates what the HUD editor draws. */
	public HudPreview createPreview() {
		return this.preview.get();
	}

	@Override
	public String formatValue(HudPosition value) {
		String[] horizontal = {"left", "center", "right"};
		String[] vertical = {"top", "center", "bottom"};
		return String.format(Locale.ROOT, "%s %s (%+d, %+d)",
			vertical[value.anchorY()], horizontal[value.anchorX()], value.offsetX(), value.offsetY());
	}

	@Override
	public JsonElement toJson(HudPosition value) {
		JsonObject json = new JsonObject();
		json.addProperty("anchorX", value.anchorX());
		json.addProperty("anchorY", value.anchorY());
		json.addProperty("offsetX", value.offsetX());
		json.addProperty("offsetY", value.offsetY());
		return json;
	}

	@Override
	public HudPosition fromJson(JsonElement json) {
		JsonObject object = json.getAsJsonObject();
		return new HudPosition(
			object.get("anchorX").getAsInt(),
			object.get("anchorY").getAsInt(),
			object.get("offsetX").getAsInt(),
			object.get("offsetY").getAsInt()
		);
	}
}
