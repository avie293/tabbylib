package me.avie29.tabbylib.client.gui;

import me.avie29.tabbylib.api.option.ColorOption;
import me.avie29.tabbylib.client.gui.widget.ColorSwatch;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import me.avie29.tabbylib.compat.McCompat;
import me.avie29.tabbylib.compat.TabbyScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** HSV color picker with optional alpha, hex input and preset colors (legacy variant). */
public class ColorPickerScreen extends TabbyScreen {
	private static final int SQUARE = 100;
	private static final int BAR = 12;
	private static final int PRESET = 12;
	private static final int[] PRESETS = presets();

	private final Screen parent;
	private final int initialColor;
	private final boolean alpha;
	private final IntConsumer onDone;

	private float hue;
	private float saturation;
	private float brightness;
	private float opacity;

	private EditBox hexBox;
	/** Until the user changes something the exact initial color is kept (HSV conversion can round). */
	private boolean touched;
	private boolean updatingHex;
	private Drag drag = Drag.NONE;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	private enum Drag {
		NONE, SQUARE, HUE, ALPHA
	}

	public ColorPickerScreen(Screen parent, Component optionName, int color, boolean alpha, IntConsumer onDone) {
		super(Component.translatable("tabbylib.color.title", optionName));
		this.parent = parent;
		this.initialColor = color;
		this.alpha = alpha;
		this.onDone = onDone;
		this.setFromArgb(color);
	}

	private int squareX() {
		return this.panelX + 10;
	}

	private int squareY() {
		return this.panelY + 24;
	}

	private int hueX() {
		return this.squareX() + SQUARE + 6;
	}

	private int alphaX() {
		return this.hueX() + BAR + 4;
	}

	private int sideX() {
		return (this.alpha ? this.alphaX() + BAR : this.hueX() + BAR) + 10;
	}

	@Override
	protected void init() {
		this.panelWidth = this.alpha ? 290 : 274;
		this.panelHeight = 176;
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;

		int sideWidth = this.panelX + this.panelWidth - 10 - this.sideX();
		this.hexBox = new EditBox(this.font, this.sideX() + 1, this.squareY() + 45, sideWidth - 2, 16, Component.translatable("tabbylib.color.hex"));
		this.hexBox.setMaxLength(9);
		this.hexBox.setResponder(text -> {
			if (this.updatingHex) {
				return;
			}
			Integer parsed = ColorOption.parseHex(text);
			if (parsed != null) {
				this.touched = true;
				this.setFromArgb(this.alpha ? parsed : parsed | 0xFF000000);
				this.hexBox.setTextColor(0xFFE0E0E0);
			} else {
				this.hexBox.setTextColor(0xFFFF5555);
			}
		});
		this.addRenderableWidget(this.hexBox);
		this.updateHex();

		int buttonY = this.panelY + this.panelHeight - 28;
		int buttonWidth = (this.panelWidth - 24) / 2;
		TabbyButton cancel = new TabbyButton(buttonWidth, 20, Component.translatable("gui.cancel"), this::onClose);
		cancel.setPosition(this.panelX + 10, buttonY);
		TabbyButton done = new TabbyButton(buttonWidth, 20, Component.translatable("gui.done"), () -> {
			this.onDone.accept(this.currentColor());
			this.onClose();
		});
		done.setPosition(this.panelX + 14 + buttonWidth, buttonY);
		this.addRenderableWidget(cancel);
		this.addRenderableWidget(done);
	}

	@Override
	public void onClose() {
		McCompat.setScreen(this.parent);
	}

	// ---------------------------------------------------------------- color math

	private int currentColor() {
		if (!this.touched) {
			return this.initialColor;
		}
		int rgb = Mth.hsvToRgb(this.hue, this.saturation, this.brightness) & 0xFFFFFF;
		int a = this.alpha ? Math.round(this.opacity * 255) : 255;
		return a << 24 | rgb;
	}

	private void setFromArgb(int argb) {
		float r = (argb >> 16 & 0xFF) / 255f;
		float g = (argb >> 8 & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;

		if (delta > 0) {
			float h;
			if (max == r) {
				h = ((g - b) / delta) % 6;
			} else if (max == g) {
				h = (b - r) / delta + 2;
			} else {
				h = (r - g) / delta + 4;
			}
			h /= 6;
			if (h < 0) {
				h += 1;
			}
			// Keeps the hue when picking grey tones
			this.hue = h;
		}
		this.saturation = max == 0 ? 0 : delta / max;
		this.brightness = max;
		this.opacity = (argb >>> 24) / 255f;
	}

	private void updateHex() {
		this.updatingHex = true;
		this.hexBox.setValue(ColorOption.toHex(this.currentColor(), this.alpha));
		this.hexBox.setTextColor(0xFFE0E0E0);
		this.updatingHex = false;
	}

	/** The 16 chat colors. */
	private static int[] presets() {
		return new int[]{
			0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
			0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
		};
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int top = this.squareY();
			if (mouseY >= top && mouseY < top + SQUARE) {
				if (mouseX >= this.squareX() && mouseX < this.squareX() + SQUARE) {
					this.drag = Drag.SQUARE;
				} else if (mouseX >= this.hueX() && mouseX < this.hueX() + BAR) {
					this.drag = Drag.HUE;
				} else if (this.alpha && mouseX >= this.alphaX() && mouseX < this.alphaX() + BAR) {
					this.drag = Drag.ALPHA;
				}
			}
			if (this.drag != Drag.NONE) {
				this.updateDrag(mouseX, mouseY);
				return true;
			}

			int preset = this.presetAt(mouseX, mouseY);
			if (preset >= 0) {
				int keepAlpha = this.alpha ? Math.round(this.opacity * 255) << 24 : 0xFF000000;
				this.touched = true;
				this.setFromArgb(keepAlpha | (PRESETS[preset] & 0xFFFFFF));
				this.updateHex();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.drag != Drag.NONE) {
			this.updateDrag(mouseX, mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.drag = Drag.NONE;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void updateDrag(double mx, double my) {
		this.touched = true;
		float relY = Mth.clamp((float) (my - this.squareY()) / (SQUARE - 1), 0, 1);
		switch (this.drag) {
			case SQUARE -> {
				this.saturation = Mth.clamp((float) (mx - this.squareX()) / (SQUARE - 1), 0, 1);
				this.brightness = 1 - relY;
			}
			case HUE -> this.hue = Math.min(relY, 0.9999f);
			case ALPHA -> this.opacity = 1 - relY;
			default -> {
			}
		}
		this.updateHex();
	}

	private int presetAt(double mx, double my) {
		int startX = this.sideX();
		int startY = this.squareY() + 68;
		int columns = 8;
		for (int i = 0; i < PRESETS.length; i++) {
			int x = startX + (i % columns) * (PRESET + 2);
			int y = startY + (i / columns) * (PRESET + 2);
			if (mx >= x && mx < x + PRESET && my >= y && my < y + PRESET) {
				return i;
			}
		}
		return -1;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		Panel.draw(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
		graphics.drawString(this.font, OptionListWidget.clip(this.font, this.title, this.panelWidth - 20), this.panelX + 10, this.panelY + 9, 0xFFFFFFFF, true);

		int sx = this.squareX();
		int sy = this.squareY();

		// Saturation (x) / brightness (y) square
		for (int col = 0; col < SQUARE; col++) {
			int top = 0xFF000000 | Mth.hsvToRgb(this.hue, col / (float) (SQUARE - 1), 1);
			graphics.fillGradient(sx + col, sy, sx + col + 1, sy + SQUARE, top, 0xFF000000);
		}
		int markerX = sx + Math.round(this.saturation * (SQUARE - 1));
		int markerY = sy + Math.round((1 - this.brightness) * (SQUARE - 1));
		graphics.renderOutline(markerX - 2, markerY - 2, 5, 5, 0xFFFFFFFF);
		graphics.renderOutline(markerX - 3, markerY - 3, 7, 7, 0xFF000000);

		// Hue bar
		int hx = this.hueX();
		for (int i = 0; i < 6; i++) {
			int y0 = sy + SQUARE * i / 6;
			int y1 = sy + SQUARE * (i + 1) / 6;
			int c0 = 0xFF000000 | Mth.hsvToRgb(i / 6f, 1, 1);
			int c1 = 0xFF000000 | Mth.hsvToRgb(((i + 1) % 6) / 6f, 1, 1);
			graphics.fillGradient(hx, y0, hx + BAR, y1, c0, c1);
		}
		int hueY = sy + Math.round(this.hue * (SQUARE - 1));
		graphics.fill(hx - 1, hueY - 1, hx + BAR + 1, hueY + 2, 0xFFFFFFFF);
		graphics.fill(hx, hueY, hx + BAR, hueY + 1, 0xFF000000);

		// Alpha bar
		if (this.alpha) {
			int ax = this.alphaX();
			int opaque = 0xFF000000 | Mth.hsvToRgb(this.hue, this.saturation, this.brightness);
			ColorSwatch.checkerboard(graphics, ax, sy, ax + BAR, sy + SQUARE);
			graphics.fillGradient(ax, sy, ax + BAR, sy + SQUARE, opaque, opaque & 0x00FFFFFF);
			int alphaY = sy + Math.round((1 - this.opacity) * (SQUARE - 1));
			graphics.fill(ax - 1, alphaY - 1, ax + BAR + 1, alphaY + 2, 0xFFFFFFFF);
			graphics.fill(ax, alphaY, ax + BAR, alphaY + 1, 0xFF000000);
		}

		// Old / new preview
		int px = this.sideX();
		int previewWidth = (this.panelX + this.panelWidth - 10 - px) / 2;
		graphics.drawString(this.font, Component.translatable("tabbylib.color.new"), px, sy, 0xFFA0A0A0, false);
		graphics.drawString(this.font, Component.translatable("tabbylib.color.old"), px + previewWidth, sy, 0xFFA0A0A0, false);
		graphics.fill(px, sy + 11, px + previewWidth * 2, sy + 37, 0xFF000000);
		ColorSwatch.drawColor(graphics, px + 1, sy + 12, px + previewWidth, sy + 36, this.currentColor());
		ColorSwatch.drawColor(graphics, px + previewWidth, sy + 12, px + previewWidth * 2 - 1, sy + 36, this.initialColor);

		// Presets
		int startY = sy + 68;
		for (int i = 0; i < PRESETS.length; i++) {
			int x = px + (i % 8) * (PRESET + 2);
			int y = startY + (i / 8) * (PRESET + 2);
			boolean hovered = mouseX >= x && mouseX < x + PRESET && mouseY >= y && mouseY < y + PRESET;
			graphics.fill(x - 1, y - 1, x + PRESET + 1, y + PRESET + 1, hovered ? 0xFFFFFFFF : 0xFF000000);
			graphics.fill(x, y, x + PRESET, y + PRESET, PRESETS[i]);
		}
	}
}
