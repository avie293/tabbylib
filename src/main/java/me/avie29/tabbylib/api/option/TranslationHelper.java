package me.avie29.tabbylib.api.option;

import net.minecraft.locale.Language;

/** Checks whether a translation key exists in the current language. */
final class TranslationHelper {
	private TranslationHelper() {
	}

	static boolean exists(String key) {
		return Language.getInstance().has(key);
	}
}
