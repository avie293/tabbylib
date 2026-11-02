package cc.avie.tabbylib;

import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.option.BooleanOption;
import cc.avie.tabbylib.api.option.ColorOption;
import cc.avie.tabbylib.api.option.IntOption;
import cc.avie.tabbylib.api.option.Options;

public final class TabbyLibConfig {
	public static final BooleanOption OPTIONS_BUTTON = Options.toggle("optionsButton", true);
	public static final BooleanOption CONFIRM_UNSAVED = Options.toggle("confirmUnsaved", true);
	public static final ColorOption ACCENT_COLOR = Options.color("accentColor", 0xF2A541);

	public static final BooleanOption SNAP_EDGES = Options.toggle("snapEdges", true);
	public static final BooleanOption SNAP_GRID = Options.toggle("snapGrid", true);
	public static final IntOption GRID_SIZE = Options.integer("gridSize", 10).slider(2, 40, 1).dependsOn(SNAP_GRID);
	public static final BooleanOption SHOW_GRID = Options.toggle("showGrid", false).dependsOn(SNAP_GRID);
	public static final IntOption SNAP_DISTANCE = Options.integer("snapDistance", 8).slider(1, 20, 1);

	private TabbyLibConfig() {
	}

	static void init() {
		TabbyConfig.builder(TabbyLib.MOD_ID)
			.category("general", category -> category
				.add(OPTIONS_BUTTON, CONFIRM_UNSAVED)
				.section("appearance", section -> section.add(ACCENT_COLOR)))
			.category("hudEditor", category -> category
				.add(SNAP_EDGES, SNAP_GRID, GRID_SIZE, SHOW_GRID, SNAP_DISTANCE))
			.build();
	}

	public static int accentColor() {
		return ACCENT_COLOR.getPending();
	}
}
