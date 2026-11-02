package cc.avie.tabbylib.platform;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/** What TabbyLib needs from the mod loader, implemented once per loader and found with {@link ServiceLoader}. */
public interface Platform {
	static Platform get() {
		return Holder.INSTANCE;
	}

	/** The {@code config} folder of the game. */
	Path configDir();

	/** Display name from the mod metadata, empty when the mod is unknown. */
	Optional<String> modName(String modId);

	/** Version from the mod metadata, empty when the mod is unknown. */
	Optional<String> modVersion(String modId);

	/** Opens the icon from the mod metadata, null when the mod has none. */
	@Nullable InputStream openModIcon(String modId) throws IOException;

	final class Holder {
		private static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No TabbyLib platform found, the jar of the wrong loader is installed"));

		private Holder() {
		}
	}
}
