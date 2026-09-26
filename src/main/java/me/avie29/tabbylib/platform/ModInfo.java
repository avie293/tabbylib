package me.avie29.tabbylib.platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Loader independent information about an installed mod.
 *
 * @param iconPath path of the icon / logo inside the mod jar, e.g. "assets/mymod/icon.png"
 */
public record ModInfo(String id, String name, String version, Optional<String> iconPath, ResourceOpener resources) {
	/** Opens a file inside the mod jar. */
	@FunctionalInterface
	public interface ResourceOpener {
		Optional<InputStream> open(String path) throws IOException;
	}
}
