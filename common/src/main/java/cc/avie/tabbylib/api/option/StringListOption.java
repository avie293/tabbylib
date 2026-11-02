package cc.avie.tabbylib.api.option;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Editable list of strings, created with {@link Options#textList}. */
public class StringListOption extends Option<List<String>, StringListOption> {
	private int maxEntries = 256;

	protected StringListOption(String key, List<String> defaultValue) {
		super(key, List.copyOf(defaultValue));
	}

	/** Largest number of entries (default 256). */
	public StringListOption maxEntries(int maxEntries) {
		return this.configure(() -> this.maxEntries = maxEntries);
	}

	public int getMaxEntries() {
		return this.maxEntries;
	}

	@Override
	protected List<String> validate(List<String> value) {
		return List.copyOf(value.size() > this.maxEntries ? value.subList(0, this.maxEntries) : value);
	}

	@Override
	public String formatValue(List<String> value) {
		return String.join(", ", value);
	}

	@Override
	public JsonElement toJson(List<String> value) {
		JsonArray array = new JsonArray();
		value.forEach(array::add);
		return array;
	}

	@Override
	public List<String> fromJson(JsonElement json) {
		List<String> list = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray()) {
			list.add(element.getAsString());
		}
		return list;
	}
}
