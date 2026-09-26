package me.avie29.tabbylib.api.option;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** An editable list of strings, e.g. player names or server addresses. */
public class StringListOption extends Option<List<String>> {
	private final int maxEntries;

	protected StringListOption(Builder builder) {
		super(builder);
		this.maxEntries = builder.maxEntries;
	}

	public static Builder builder(String key, List<String> defaultValue) {
		return new Builder(key, List.copyOf(defaultValue));
	}

	public int getMaxEntries() {
		return this.maxEntries;
	}

	@Override
	protected List<String> validate(List<String> value) {
		List<String> copy = value.size() > this.maxEntries ? value.subList(0, this.maxEntries) : value;
		return List.copyOf(copy);
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

	public static class Builder extends Option.Builder<List<String>, Builder> {
		private int maxEntries = 256;

		protected Builder(String key, List<String> defaultValue) {
			super(key, defaultValue);
		}

		public Builder maxEntries(int maxEntries) {
			this.maxEntries = maxEntries;
			return this;
		}

		@Override
		public StringListOption build() {
			return new StringListOption(this);
		}
	}
}
