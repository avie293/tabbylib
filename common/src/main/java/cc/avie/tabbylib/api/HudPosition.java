package cc.avie.tabbylib.api;

import net.minecraft.util.Mth;

/** Screen position of a HUD element that survives window resizes and GUI scale changes. */
public record HudPosition(int anchorX, int anchorY, int offsetX, int offsetY) {
	public static final int START = 0;
	public static final int CENTER = 1;
	public static final int END = 2;

	public HudPosition {
		anchorX = Mth.clamp(anchorX, START, END);
		anchorY = Mth.clamp(anchorY, START, END);
	}

	/** Same as the constructor. */
	public static HudPosition of(int anchorX, int anchorY, int offsetX, int offsetY) {
		return new HudPosition(anchorX, anchorY, offsetX, offsetY);
	}

	/** Left x coordinate of an element with the given width. */
	public int x(int screenWidth, int elementWidth) {
		return resolve(this.anchorX, this.offsetX, screenWidth, elementWidth);
	}

	/** Top y coordinate of an element with the given height. */
	public int y(int screenHeight, int elementHeight) {
		return resolve(this.anchorY, this.offsetY, screenHeight, elementHeight);
	}

	/** Creates a position from absolute coordinates, attached to the closest anchor. */
	public static HudPosition fromAbsolute(int x, int y, int screenWidth, int screenHeight, int elementWidth, int elementHeight) {
		int maxX = Math.max(0, screenWidth - elementWidth);
		int maxY = Math.max(0, screenHeight - elementHeight);
		x = Mth.clamp(x, 0, maxX);
		y = Mth.clamp(y, 0, maxY);
		int anchorX = closestAnchor(x, maxX);
		int anchorY = closestAnchor(y, maxY);
		return new HudPosition(anchorX, anchorY, x - anchorCoordinate(anchorX, maxX), y - anchorCoordinate(anchorY, maxY));
	}

	private static int resolve(int anchor, int offset, int screenSize, int elementSize) {
		int max = Math.max(0, screenSize - elementSize);
		return Mth.clamp(anchorCoordinate(anchor, max) + offset, 0, max);
	}

	private static int anchorCoordinate(int anchor, int max) {
		return switch (anchor) {
			case START -> 0;
			case CENTER -> max / 2;
			default -> max;
		};
	}

	private static int closestAnchor(int value, int max) {
		int toStart = value;
		int toCenter = Math.abs(value - max / 2);
		int toEnd = Math.abs(max - value);
		if (toStart <= toCenter && toStart <= toEnd) {
			return START;
		}
		return toCenter <= toEnd ? CENTER : END;
	}
}
