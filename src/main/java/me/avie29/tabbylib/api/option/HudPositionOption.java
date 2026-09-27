package me.avie29.tabbylib.api.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.avie29.tabbylib.api.HudPosition;
import me.avie29.tabbylib.api.HudPreview;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Position of a HUD element. The config screen shows an "Edit position" button that opens a
 * drag and drop editor with snapping to the screen edges, the center and a grid.
 */
public class HudPositionOption extends Option<HudPosition> {
	private final Supplier<HudPreview> preview;

	protected HudPositionOption(Builder builder) {
		super(builder);
		this.preview = builder.preview;
	}

	/**
	 * Starts a new HUD position option.
	 *
	 * @param key          unique key inside the config, used in the file and in the translation key
	 * @param defaultValue where the element is before the player moves it
	 * @param preview      creates the preview drawn in the editor. It is a supplier so the class is only
	 *                     touched on the client when the editor opens.
	 */
	public static Builder builder(String key, HudPosition defaultValue, Supplier<HudPreview> preview) {
		return new Builder(key, defaultValue, preview);
	}

	/** Creates the preview drawn in the HUD editor. */
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

	/** Builder for {@link HudPositionOption}. The shared settings are in {@link Option.Builder}. */
	public static class Builder extends Option.Builder<HudPosition, Builder> {
		private final Supplier<HudPreview> preview;

		protected Builder(String key, HudPosition defaultValue, Supplier<HudPreview> preview) {
			super(key, defaultValue);
			this.preview = preview;
		}

		@Override
		public HudPositionOption build() {
			return new HudPositionOption(this);
		}
	}
}
