package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.ActionEntry;
import me.avie29.tabbylib.api.OptionGroup;
import me.avie29.tabbylib.api.option.Option;
import me.avie29.tabbylib.client.gui.entry.OptionControl;
import me.avie29.tabbylib.client.gui.entry.OptionControls;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Scrollable list of option rows on the right side of the config screen. */
public class OptionListWidget extends ContainerObjectSelectionList<OptionListWidget.Row> {
	public static final int ROW_HEIGHT = 24;
	private static final int INDENT = 10;
	private static final int RESET_WIDTH = 20;

	private final TabbyConfigScreen screen;

	public OptionListWidget(Minecraft minecraft, TabbyConfigScreen screen, int x, int y, int width, int height) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.screen = screen;
		this.setX(x);
		this.centerListVertically = false;
	}

	public void clear() {
		this.clearEntries();
	}

	public void addOption(Option<?> option, boolean indented) {
		this.addEntry(new OptionRow(option, OptionControls.create(option, this.screen), indented));
	}

	public void addGroup(OptionGroup group) {
		this.addEntry(new GroupRow(group), 20);
	}

	public void addAction(ActionEntry action, boolean indented) {
		this.addEntry(new ActionRow(action, indented));
	}

	public void addHeader(Component text) {
		this.addEntry(new HeaderRow(text), 16);
	}

	public void addText(Component text, boolean indented) {
		Font font = this.minecraft.font;
		int wrapWidth = Math.max(40, this.getRowWidth() - 8 - (indented ? INDENT : 0));
		List<FormattedCharSequence> lines = font.split(text, wrapWidth);
		this.addEntry(new TextRow(lines, indented), Math.max(12, lines.size() * (font.lineHeight + 1) + 4));
	}

	/** Updates all controls from the pending values (after reset / discard). */
	public void refreshControls() {
		for (Row row : this.children()) {
			if (row instanceof OptionRow optionRow) {
				optionRow.control.refresh();
			}
		}
	}

	@Override
	public int getRowWidth() {
		return this.width - 14;
	}

	@Override
	public int getRowLeft() {
		return this.getX() + 4;
	}

	@Override
	protected int scrollBarX() {
		return this.getRight() - 7;
	}

	@Override
	protected void extractListBackground(GuiGraphicsExtractor graphics) {
		// The screen draws the panel
	}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor graphics) {
	}

	// ---------------------------------------------------------------- rows

	public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
	}

	private final class OptionRow extends Row {
		private final Option<?> option;
		private final OptionControl control;
		private final TabbyButton resetButton;
		private final List<AbstractWidget> widgets = new ArrayList<>();
		private final boolean indented;

		OptionRow(Option<?> option, OptionControl control, boolean indented) {
			this.option = option;
			this.control = control;
			this.indented = indented;
			this.resetButton = new TabbyButton(RESET_WIDTH, 20, Component.literal("↺"), () -> {
				option.resetPending();
				control.refresh();
			});
			this.resetButton.setTooltip(Tooltip.create(Component.translatable("tabbylib.reset.tooltip",
				Component.literal(describeDefault(option)).withStyle(ChatFormatting.YELLOW))));
			this.widgets.addAll(control.widgets());
			this.widgets.add(this.resetButton);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = OptionListWidget.this.minecraft.font;
			int left = this.getContentX() + (this.indented ? INDENT : 0);
			int right = this.getContentRight();
			int top = this.getY() + 2;
			int height = 20;
			int controlWidth = Math.min(150, Math.max(80, (right - left) * 45 / 100));
			int controlX = right - RESET_WIDTH - 2 - controlWidth;

			boolean enabled = this.option.isEnabled();
			this.control.setActive(enabled);
			this.control.layout(controlX, top, controlWidth, height);
			this.resetButton.setPosition(right - RESET_WIDTH, top);
			this.resetButton.active = enabled && !this.option.isPendingDefault();

			if (hovered) {
				graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x18FFFFFF);
			}

			// Modified marker
			if (this.option.isDirty()) {
				graphics.fill(left - 3, top + 3, left - 1, top + height - 3, TabbyLibConfig.accentColor());
			}

			MutableComponent name = this.option.getName().copy();
			if (this.option.requiresRestart()) {
				name.append(Component.literal(" *").withStyle(ChatFormatting.RED));
			}
			int labelWidth = controlX - left - 6;
			int color = !enabled ? 0xFF808080 : this.option.isDirty() ? 0xFFFFE08A : 0xFFFFFFFF;
			FormattedCharSequence label = clip(font, name, labelWidth);
			graphics.text(font, label, left + 2, top + (height - 8) / 2, color, true);

			for (AbstractWidget widget : this.widgets) {
				widget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			boolean overLabel = mouseX >= left && mouseX < controlX - 4 && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
			if (overLabel) {
				graphics.setTooltipForNextFrame(font, this.tooltipLines(font), mouseX, mouseY);
			}
		}

		private List<FormattedCharSequence> tooltipLines(Font font) {
			List<FormattedCharSequence> lines = new ArrayList<>();
			lines.add(this.option.getName().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());
			Component tooltip = this.option.getTooltip();
			if (tooltip != null) {
				lines.addAll(font.split(tooltip.copy().withStyle(ChatFormatting.GRAY), 220));
			}
			if (this.option.requiresRestart()) {
				lines.addAll(font.split(Component.translatable("tabbylib.requires_restart").withStyle(ChatFormatting.RED), 220));
			}
			if (!this.option.isEnabled()) {
				lines.addAll(font.split(Component.translatable("tabbylib.disabled").withStyle(ChatFormatting.DARK_GRAY), 220));
			}
			lines.add(Component.translatable("tabbylib.default", describeDefault(this.option)).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
			return lines;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return this.widgets;
		}
	}

	private final class GroupRow extends Row {
		private final OptionGroup group;

		GroupRow(OptionGroup group) {
			this.group = group;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = OptionListWidget.this.minecraft.font;
			int x = this.getContentX();
			int y = this.getY();
			int accent = TabbyLibConfig.accentColor();
			graphics.fill(x, y + this.getHeight() - 3, this.getContentRight(), y + this.getHeight() - 2, (accent & 0x00FFFFFF) | 0x80000000);
			String arrow = this.group.isCollapsed() ? "▶ " : "▼ ";
			Component text = Component.literal(arrow).withStyle(style -> style.withColor(accent & 0xFFFFFF))
				.append(this.group.getName().copy().withStyle(ChatFormatting.BOLD));
			graphics.text(font, text, x + 2, y + 6, hovered ? 0xFFFFFFFF : 0xFFE0E0E0, true);
			if (hovered && this.group.getDescription() != null) {
				graphics.setTooltipForNextFrame(font, font.split(this.group.getDescription(), 220), mouseX, mouseY);
			}
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				this.group.setCollapsed(!this.group.isCollapsed());
				AbstractWidget.playButtonClickSound(OptionListWidget.this.minecraft.getSoundManager());
				OptionListWidget.this.screen.rebuildOptions();
				return true;
			}
			return false;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(new TextNarratable(this.group.getName()));
		}
	}

	private final class ActionRow extends Row {
		private final ActionEntry action;
		private final TabbyButton button;
		private final boolean indented;

		ActionRow(ActionEntry action, boolean indented) {
			this.action = action;
			this.indented = indented;
			this.button = new TabbyButton(0, 20, action.buttonText(), action.action());
			if (action.tooltip() != null) {
				this.button.setTooltip(Tooltip.create(action.tooltip()));
			}
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = OptionListWidget.this.minecraft.font;
			int left = this.getContentX() + (this.indented ? INDENT : 0);
			int right = this.getContentRight();
			int top = this.getY() + 2;
			int buttonWidth = Math.min(150, Math.max(80, (right - left) * 45 / 100)) + RESET_WIDTH + 2;
			int buttonX = right - buttonWidth;
			this.button.setRectangle(buttonWidth, 20, buttonX, top);
			graphics.text(font, clip(font, this.action.name(), buttonX - left - 6), left + 2, top + 6, 0xFFFFFFFF, true);
			this.button.extractRenderState(graphics, mouseX, mouseY, a);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.button);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.button);
		}
	}

	private final class HeaderRow extends Row {
		private final Component text;

		HeaderRow(Component text) {
			this.text = text;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = OptionListWidget.this.minecraft.font;
			graphics.text(font, this.text, this.getContentX() + 2, this.getY() + 5, TabbyLibConfig.accentColor(), true);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(new TextNarratable(this.text));
		}
	}

	private final class TextRow extends Row {
		private final List<FormattedCharSequence> lines;
		private final boolean indented;

		TextRow(List<FormattedCharSequence> lines, boolean indented) {
			this.lines = lines;
			this.indented = indented;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = OptionListWidget.this.minecraft.font;
			int x = this.getContentX() + 2 + (this.indented ? INDENT : 0);
			int y = this.getY() + 2;
			for (FormattedCharSequence line : this.lines) {
				graphics.text(font, line, x, y, 0xFFB0B0B0, false);
				y += font.lineHeight + 1;
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	// ---------------------------------------------------------------- helpers

	private static <T> String describeDefault(Option<T> option) {
		return option.formatValue(option.getDefault());
	}

	static FormattedCharSequence clip(Font font, Component text, int width) {
		if (font.width(text) <= width) {
			return text.getVisualOrderText();
		}
		String ellipsis = "...";
		String clipped = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width(ellipsis)));
		return Component.literal(clipped + ellipsis).withStyle(text.getStyle()).getVisualOrderText();
	}

	/** Lets the narrator read rows that have no widgets. */
	private record TextNarratable(Component text) implements NarratableEntry {
		@Override
		public NarrationPriority narrationPriority() {
			return NarrationPriority.HOVERED;
		}

		@Override
		public void updateNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, this.text);
		}
	}

}
