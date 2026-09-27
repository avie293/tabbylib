package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.HudPosition;
import me.avie29.tabbylib.api.HudPreview;
import me.avie29.tabbylib.api.option.HudPositionOption;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Drag and drop editor for a {@link HudPositionOption}.
 * Snaps to the screen edges, the center and a grid. Hold Alt while dragging to place freely,
 * use the arrow keys to move by 1 pixel (10 with Shift).
 */
public class HudEditorScreen extends Screen {
	private static final int GUIDE_COLOR = 0x8033CCFF;
	private static boolean open;

	private final @Nullable Screen parent;
	private final HudPositionOption option;
	/** Opened on its own (not from the config screen): "Done" saves right away. */
	private final boolean standalone;
	private final HudPreview preview;
	private final HudPosition initial;

	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private boolean guideVertical;
	private boolean guideHorizontal;

	public HudEditorScreen(@Nullable Screen parent, HudPositionOption option) {
		this(parent, option, false);
	}

	public HudEditorScreen(@Nullable Screen parent, HudPositionOption option, boolean standalone) {
		super(Component.translatable("tabbylib.hud.title", option.getName()));
		this.parent = parent;
		this.option = option;
		this.standalone = standalone;
		this.preview = option.createPreview();
		this.initial = option.getPending();
	}

	/** True while any HUD editor is open. Mods can skip drawing their real HUD element then. */
	public static boolean isOpen() {
		return open;
	}

	@Override
	protected void init() {
		open = true;
		int buttonWidth = 70;
		int y = this.height - 28;
		int x = (this.width - buttonWidth * 3 - 8) / 2;
		TabbyButton reset = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.hud.reset"), () -> this.option.resetPending());
		TabbyButton cancel = new TabbyButton(buttonWidth, 20, Component.translatable("gui.cancel"), () -> {
			this.option.setPending(this.initial);
			this.close(false);
		});
		TabbyButton done = new TabbyButton(buttonWidth, 20, Component.translatable("gui.done"), () -> this.close(true));
		for (TabbyButton button : List.of(reset, cancel, done)) {
			button.setPosition(x, y);
			x += buttonWidth + 4;
			this.addRenderableWidget(button);
		}
	}

	@Override
	public void removed() {
		open = false;
	}

	@Override
	public void onClose() {
		this.close(true);
	}

	private void close(boolean keep) {
		open = false;
		if (this.standalone) {
			if (keep && this.option.isDirty() && this.option.getConfig() != null) {
				this.option.commit();
				this.option.getConfig().save();
			} else {
				this.option.discard();
			}
		}
		McCompat.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return this.minecraft.level != null;
	}

	// ---------------------------------------------------------------- position helpers

	private int elementX() {
		return this.option.getPending().x(this.width, this.preview.width());
	}

	private int elementY() {
		return this.option.getPending().y(this.height, this.preview.height());
	}

	private boolean isOverElement(double mx, double my) {
		int x = this.elementX();
		int y = this.elementY();
		return mx >= x - 2 && mx < x + this.preview.width() + 2 && my >= y - 2 && my < y + this.preview.height() + 2;
	}

	private void moveTo(int x, int y) {
		this.option.setPending(HudPosition.fromAbsolute(x, y, this.width, this.height, this.preview.width(), this.preview.height()));
	}

	private int snap(int value, int max, boolean vertical, boolean free) {
		value = Math.max(0, Math.min(value, max));
		boolean guide = false;
		if (!free) {
			int distance = TabbyLibConfig.SNAP_DISTANCE.get();
			int best = value;
			int bestDistance = distance + 1;
			if (TabbyLibConfig.SNAP_EDGES.get()) {
				for (int target : new int[]{0, max, max / 2}) {
					int d = Math.abs(value - target);
					if (d <= distance && d < bestDistance) {
						best = target;
						bestDistance = d;
						guide = true;
					}
				}
			}
			if (!guide && TabbyLibConfig.SNAP_GRID.get()) {
				int grid = TabbyLibConfig.GRID_SIZE.get();
				int target = Math.round((float) value / grid) * grid;
				if (Math.abs(value - target) <= distance) {
					best = Math.min(target, max);
				}
			}
			value = best;
		}
		if (vertical) {
			this.guideHorizontal = guide;
		} else {
			this.guideVertical = guide;
		}
		return value;
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.isOverElement(event.x(), event.y())) {
			this.dragging = true;
			this.dragOffsetX = (int) event.x() - this.elementX();
			this.dragOffsetY = (int) event.y() - this.elementY();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.dragging) {
			boolean free = event.hasAltDown();
			int maxX = Math.max(0, this.width - this.preview.width());
			int maxY = Math.max(0, this.height - this.preview.height());
			int x = this.snap((int) event.x() - this.dragOffsetX, maxX, false, free);
			int y = this.snap((int) event.y() - this.dragOffsetY, maxY, true, free);
			this.moveTo(x, y);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			this.dragging = false;
			this.guideHorizontal = false;
			this.guideVertical = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int step = event.hasShiftDown() ? 10 : 1;
		int dx = event.isLeft() ? -step : event.isRight() ? step : 0;
		int dy = event.isUp() ? -step : event.isDown() ? step : 0;
		if (dx != 0 || dy != 0) {
			this.moveTo(this.elementX() + dx, this.elementY() + dy);
			return true;
		}
		return super.keyPressed(event);
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (this.minecraft.level != null) {
			// Keep the world visible so the element can be placed in context
			graphics.fill(0, 0, this.width, this.height, 0x30000000);
		} else {
			super.extractBackground(graphics, mouseX, mouseY, a);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (TabbyLibConfig.SHOW_GRID.get() && TabbyLibConfig.SNAP_GRID.get()) {
			int grid = TabbyLibConfig.GRID_SIZE.get();
			for (int x = grid; x < this.width; x += grid) {
				graphics.fill(x, 0, x + 1, this.height, 0x18FFFFFF);
			}
			for (int y = grid; y < this.height; y += grid) {
				graphics.fill(0, y, this.width, y + 1, 0x18FFFFFF);
			}
		}

		int x = this.elementX();
		int y = this.elementY();
		int w = this.preview.width();
		int h = this.preview.height();

		if (this.dragging) {
			if (this.guideVertical) {
				int guideX = x + w / 2;
				graphics.fill(guideX, 0, guideX + 1, this.height, GUIDE_COLOR);
			}
			if (this.guideHorizontal) {
				int guideY = y + h / 2;
				graphics.fill(0, guideY, this.width, guideY + 1, GUIDE_COLOR);
			}
		}

		this.preview.render(graphics, x, y);
		boolean hovered = this.dragging || this.isOverElement(mouseX, mouseY);
		graphics.outline(x - 1, y - 1, w + 2, h + 2, hovered ? 0xFFFFFFFF : TabbyLibConfig.accentColor());

		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("tabbylib.hud.hint"), this.width / 2, 24, 0xFFAAAAAA);
		graphics.centeredText(this.font, Component.literal(this.option.formatValue(this.option.getPending())), this.width / 2, this.height - 40, 0xFF808080);

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
}
