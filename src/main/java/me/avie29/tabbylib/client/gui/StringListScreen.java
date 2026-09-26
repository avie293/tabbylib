package me.avie29.tabbylib.client.gui;

import me.avie29.tabbylib.api.option.StringListOption;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Edits a {@link StringListOption}: one text field per entry, with add / remove / move buttons. */
public class StringListScreen extends Screen {
	private final Screen parent;
	private final StringListOption option;
	private final List<String> values;
	private EntryList list;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	public StringListScreen(Screen parent, StringListOption option) {
		super(option.getName());
		this.parent = parent;
		this.option = option;
		this.values = new ArrayList<>(option.getPending());
	}

	@Override
	protected void init() {
		this.panelWidth = Mth.clamp(this.width - 40, 200, 340);
		this.panelHeight = this.height - 40;
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = 20;

		double scroll = this.list != null ? this.list.scrollAmount() : 0;
		this.list = new EntryList(this.minecraft, this.panelX + 2, this.panelY + 22, this.panelWidth - 4, this.panelHeight - 52);
		this.addRenderableWidget(this.list);
		this.list.setScrollAmount(scroll);

		int buttonY = this.panelY + this.panelHeight - 26;
		int buttonWidth = (this.panelWidth - 28) / 3;
		TabbyButton addButton = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.list.add"), () -> {
			this.values.add("");
			this.rebuildWidgets();
			this.list.setScrollAmount(this.list.maxScrollAmount());
		});
		addButton.active = this.values.size() < this.option.getMaxEntries();
		TabbyButton cancel = new TabbyButton(buttonWidth, 20, Component.translatable("gui.cancel"), this::onClose);
		TabbyButton done = new TabbyButton(buttonWidth, 20, Component.translatable("gui.done"), () -> {
			this.option.setPending(this.values.stream().filter(value -> !value.isBlank()).toList());
			this.onClose();
		});
		int x = this.panelX + 10;
		for (TabbyButton button : List.of(addButton, cancel, done)) {
			button.setPosition(x, buttonY);
			x += buttonWidth + 4;
			this.addRenderableWidget(button);
		}
	}

	@Override
	public void onClose() {
		McCompat.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Panel.draw(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
		graphics.text(this.font, OptionListWidget.clip(this.font, this.title, this.panelWidth - 20), this.panelX + 10, this.panelY + 8, 0xFFFFFFFF, true);
		String count = this.values.size() + " / " + this.option.getMaxEntries();
		graphics.text(this.font, count, this.panelX + this.panelWidth - 10 - this.font.width(count), this.panelY + 8, 0xFF808080, false);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private class EntryList extends ContainerObjectSelectionList<EntryList.Row> {
		EntryList(Minecraft minecraft, int x, int y, int width, int height) {
			super(minecraft, width, height, y, 24);
			this.setX(x);
			this.centerListVertically = false;
			for (int i = 0; i < StringListScreen.this.values.size(); i++) {
				this.addEntry(new Row(i));
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
		}

		@Override
		protected void extractListSeparators(GuiGraphicsExtractor graphics) {
		}

		private class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final EditBox box;
			private final TabbyButton up;
			private final TabbyButton down;
			private final TabbyButton remove;

			Row(int index) {
				List<String> values = StringListScreen.this.values;
				this.box = new EditBox(StringListScreen.this.font, 0, 18, Component.literal("#" + (index + 1)));
				this.box.setMaxLength(1024);
				this.box.setValue(values.get(index));
				this.box.setResponder(text -> values.set(index, text));
				this.up = new TabbyButton(16, 20, Component.literal("▲"), () -> this.move(index, -1));
				this.up.active = index > 0;
				this.down = new TabbyButton(16, 20, Component.literal("▼"), () -> this.move(index, 1));
				this.down.active = index < values.size() - 1;
				this.remove = new TabbyButton(20, 20, Component.literal("✕"), () -> {
					values.remove(index);
					StringListScreen.this.rebuildWidgets();
				});
				this.remove.setTooltip(Tooltip.create(Component.translatable("tabbylib.list.remove")));
			}

			private void move(int index, int direction) {
				List<String> values = StringListScreen.this.values;
				int target = index + direction;
				if (target >= 0 && target < values.size()) {
					values.set(index, values.set(target, values.get(index)));
					StringListScreen.this.rebuildWidgets();
				}
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				int x = this.getContentX();
				int y = this.getY() + 2;
				int right = this.getContentRight();
				this.remove.setPosition(right - 20, y);
				this.down.setPosition(right - 38, y);
				this.up.setPosition(right - 56, y);
				this.box.setRectangle(right - 60 - x - 2, 18, x + 1, y + 1);
				this.box.extractRenderState(graphics, mouseX, mouseY, a);
				this.up.extractRenderState(graphics, mouseX, mouseY, a);
				this.down.extractRenderState(graphics, mouseX, mouseY, a);
				this.remove.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.box, this.up, this.down, this.remove);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.box, this.up, this.down, this.remove);
			}
		}
	}
}
