package me.avie29.tabbylib;

import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.api.option.BooleanOption;
import me.avie29.tabbylib.api.option.ColorOption;
import me.avie29.tabbylib.api.option.EnumOption;
import me.avie29.tabbylib.api.option.IntOption;

/** TabbyLib's own settings. Also a small example of the API. */
public final class TabbyLibConfig {
	public enum ListWidth {
		NARROW(90, 120),
		NORMAL(110, 160),
		WIDE(140, 210);

		public final int min;
		public final int max;

		ListWidth(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	public static final BooleanOption OPTIONS_BUTTON = BooleanOption.builder("optionsButton", true).build();
	public static final ColorOption ACCENT_COLOR = ColorOption.builder("accentColor", 0xFFF2A541).build();
	public static final BooleanOption MOD_ICONS = BooleanOption.builder("modIcons", true).build();
	public static final EnumOption<ListWidth> LIST_WIDTH = EnumOption.builder("listWidth", ListWidth.NORMAL).build();
	public static final BooleanOption CONFIRM_UNSAVED = BooleanOption.builder("confirmUnsaved", true).build();

	public static final BooleanOption SNAP_EDGES = BooleanOption.builder("snapEdges", true).build();
	public static final BooleanOption SNAP_GRID = BooleanOption.builder("snapGrid", true).build();
	public static final IntOption GRID_SIZE = IntOption.builder("gridSize", 10)
		.slider(2, 40, 1)
		.dependsOn(SNAP_GRID)
		.build();
	public static final IntOption SNAP_DISTANCE = IntOption.builder("snapDistance", 8)
		.slider(1, 20, 1)
		.build();
	public static final BooleanOption SHOW_GRID = BooleanOption.builder("showGrid", false)
		.dependsOn(SNAP_GRID)
		.build();

	private static TabbyConfig config;

	private TabbyLibConfig() {
	}

	static void init() {
		config = TabbyConfig.builder(TabbyLib.MOD_ID)
			.category("general", category -> category
				.add(OPTIONS_BUTTON, CONFIRM_UNSAVED)
				.group("appearance", group -> group.add(ACCENT_COLOR, MOD_ICONS, LIST_WIDTH)))
			.category("hudEditor", category -> category
				.add(SNAP_EDGES, SNAP_GRID, GRID_SIZE, SHOW_GRID, SNAP_DISTANCE))
			.build();
	}

	public static TabbyConfig get() {
		return config;
	}

	/** Accent color including unsaved changes, so the preview updates live. */
	public static int accentColor() {
		return ACCENT_COLOR.getPending();
	}
}
