package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.TabbyLibConfig;
import cc.avie.tabbylib.api.Category;
import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.option.Option;
import cc.avie.tabbylib.client.gui.control.OptionControls;
import cc.avie.tabbylib.client.gui.widget.CategoryTab;
import cc.avie.tabbylib.client.gui.widget.TabbyButton;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ModConfigScreen extends Screen implements OptionHost {
	private static final int MAX_WIDTH = 460;
	private static final int HEADER = 36;
	private static final int TAB_HEIGHT = 16;
	private static final int FOOTER = 32;
	private static final int LOGO_SIZE = 24;

	private final Screen parent;
	private final TabbyConfig config;
	private @Nullable Category selectedCategory;
	private String search = "";

	private @Nullable OptionList optionList;
	private @Nullable EditBox searchBox;
	private final List<CategoryTab> tabs = new ArrayList<>();
	private OptionControls.@Nullable KeyBindControl capturingKey;
	/** Whether each option was visible when the list was built, checked every tick. */
	private boolean[] visibility = new boolean[0];
	private @Nullable Component statusMessage;
	private long statusUntil;

	private int left;
	private int contentWidth;
	private int panelTop;
	private int panelBottom;

	public ModConfigScreen(Screen parent, TabbyConfig config) {
		super(config.getDisplayName());
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		this.contentWidth = Math.min(this.width - 16, MAX_WIDTH);
		this.left = (this.width - this.contentWidth) / 2;
		this.panelBottom = this.height - FOOTER;

		int searchWidth = Math.min(130, this.contentWidth / 3);
		this.searchBox = new EditBox(this.font, this.left + this.contentWidth - searchWidth - 1, 12, searchWidth, 16, Component.translatable("tabbylib.search"));
		this.searchBox.setHint(Component.translatable("tabbylib.search").withStyle(EditBox.SEARCH_HINT_STYLE));
		this.searchBox.setValue(this.search);
		this.searchBox.setResponder(text -> {
			this.search = text;
			this.rebuildTabs();
			this.rebuildOptions();
		});
		this.addRenderableWidget(this.searchBox);

		double scroll = this.optionList != null ? this.optionList.scrollAmount() : 0;
		this.optionList = new OptionList(this.minecraft, this, this.left + 2, 0, this.contentWidth - 4, 10);
		this.addRenderableWidget(this.optionList);

		int buttonWidth = Math.min(100, (this.contentWidth - 12) / 4);
		int x = (this.width - (buttonWidth * 4 + 12)) / 2;
		int y = this.height - FOOTER + 6;
		TabbyButton reset = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.reset"), () -> {
			this.config.resetPendingToDefaults();
			this.refreshAfterChange();
		});
		reset.setTooltip(Tooltip.create(Component.translatable("tabbylib.button.reset.tooltip")));
		TabbyButton discard = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.discard"), () -> {
			this.config.discard();
			this.refreshAfterChange();
			this.showStatus(Component.translatable("tabbylib.status.discarded"));
		});
		TabbyButton save = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.save"), this::save);
		TabbyButton done = new TabbyButton(buttonWidth, 20, Component.translatable("gui.done"), () -> {
			this.save();
			this.close();
		});
		for (TabbyButton button : List.of(reset, discard, save, done)) {
			button.setPosition(x, y);
			x += buttonWidth + 4;
			this.addRenderableWidget(button);
		}

		this.rebuildTabs();
		this.rebuildOptions();
		this.optionList.setScrollAmount(scroll);
	}

	private void rebuildTabs() {
		this.tabs.forEach(this::removeWidget);
		this.tabs.clear();
		List<Category> categories = this.config.getCategories();
		if (this.selectedCategory == null || !categories.contains(this.selectedCategory)) {
			this.selectedCategory = categories.getFirst();
		}
		if (categories.size() > 1 && this.search.isBlank()) {
			int x = this.left;
			int maxTabWidth = this.contentWidth / categories.size();
			for (Category category : categories) {
				int tabWidth = Math.min(maxTabWidth - 2, this.font.width(category.getName()) + 16);
				CategoryTab tab = new CategoryTab(tabWidth, TAB_HEIGHT, category.getName(), () -> this.selectedCategory == category, () -> {
					this.selectedCategory = category;
					this.rebuildOptions();
					if (this.optionList != null) {
						this.optionList.setScrollAmount(0);
					}
				});
				tab.setPosition(x, HEADER);
				x += tabWidth + 2;
				this.tabs.add(this.addRenderableWidget(tab));
			}
		}
		this.panelTop = this.tabs.isEmpty() ? HEADER : HEADER + TAB_HEIGHT + 2;
	}

	@Override
	public void rebuildOptions() {
		if (this.optionList == null) {
			return;
		}
		double scroll = this.optionList.scrollAmount();
		this.optionList.updateSizeAndPosition(this.contentWidth - 4, this.panelBottom - this.panelTop - 6, this.left + 2, this.panelTop + 3);
		this.optionList.clear();
		this.updateVisibility();

		String query = this.search.trim().toLowerCase(Locale.ROOT);
		if (!query.isEmpty()) {
			this.addSearchResults(query);
		} else if (this.selectedCategory != null) {
			this.optionList.addEntries(this.selectedCategory.getEntries(), false);
		}
		this.optionList.setScrollAmount(scroll);
	}

	private void addSearchResults(String query) {
		boolean any = false;
		for (Category category : this.config.getCategories()) {
			boolean headerAdded = false;
			for (Option<?, ?> option : category.getOptions()) {
				if (!option.isVisible() || !matches(option, query)) {
					continue;
				}
				if (!headerAdded) {
					this.optionList.addHeader(category.getName());
					headerAdded = true;
				}
				this.optionList.addOption(option, false);
				any = true;
			}
		}
		if (!any) {
			this.optionList.addText(Component.translatable("tabbylib.search.empty").withStyle(ChatFormatting.GRAY), false);
		}
	}

	private static boolean matches(Option<?, ?> option, String query) {
		if (option.getName().getString().toLowerCase(Locale.ROOT).contains(query)
			|| option.getKey().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		Component tooltip = option.getTooltip();
		return tooltip != null && tooltip.getString().toLowerCase(Locale.ROOT).contains(query);
	}

	private void updateVisibility() {
		Collection<Option<?, ?>> options = this.config.getOptions();
		if (this.visibility.length != options.size()) {
			this.visibility = new boolean[options.size()];
		}
		int i = 0;
		for (Option<?, ?> option : options) {
			this.visibility[i++] = option.isVisible();
		}
	}

	private boolean visibilityChanged() {
		Collection<Option<?, ?>> options = this.config.getOptions();
		if (this.visibility.length != options.size()) {
			return true;
		}
		int i = 0;
		for (Option<?, ?> option : options) {
			if (option.isVisible() != this.visibility[i++]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void tick() {
		if (this.visibilityChanged()) {
			this.rebuildOptions();
		}
	}

	private void save() {
		boolean changed = this.config.hasChanges();
		boolean restart = changed && this.config.commit();
		this.config.discard();
		this.refreshAfterChange();
		if (restart) {
			this.showStatus(Component.translatable("tabbylib.status.restart").withStyle(ChatFormatting.RED));
		} else if (changed) {
			this.showStatus(Component.translatable("tabbylib.status.saved").withStyle(ChatFormatting.GREEN));
		}
	}

	private void refreshAfterChange() {
		this.rebuildWidgets();
	}

	private void showStatus(Component message) {
		this.statusMessage = message;
		this.statusUntil = Util.getMillis() + 3000;
	}

	@Override
	public Screen asScreen() {
		return this;
	}

	@Override
	public void openSubScreen(Screen screen) {
		this.minecraft.gui.setScreen(screen);
	}

	@Override
	public void onClose() {
		if (this.config.hasChanges() && TabbyLibConfig.CONFIRM_UNSAVED.get()) {
			this.minecraft.gui.setScreen(new ConfirmScreen(save -> {
				if (save) {
					this.config.commit();
				}
				this.close();
			}, Component.translatable("tabbylib.unsaved.title"), Component.translatable("tabbylib.unsaved.message"),
				Component.translatable("tabbylib.button.save"), Component.translatable("tabbylib.button.discard")));
			return;
		}
		this.close();
	}

	private void close() {
		this.config.discard();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void startKeyCapture(OptionControls.KeyBindControl control) {
		OptionControls.KeyBindControl previous = this.capturingKey;
		this.capturingKey = control;
		if (previous != null && previous != control) {
			previous.refresh();
		}
	}

	@Override
	public boolean isCapturing(OptionControls.KeyBindControl control) {
		return this.capturingKey == control;
	}

	private void finishKeyCapture(InputConstants.Key key) {
		OptionControls.KeyBindControl control = this.capturingKey;
		this.capturingKey = null;
		if (control != null) {
			control.setKey(key);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.capturingKey != null) {
			this.finishKeyCapture(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		if (event.input() == InputConstants.KEY_F && event.hasControlDown() && this.searchBox != null) {
			this.setFocused(this.searchBox);
			return true;
		}
		if (event.input() == InputConstants.KEY_S && event.hasControlDown()) {
			this.save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.capturingKey != null) {
			this.finishKeyCapture(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		TabbyGui.configBackground(graphics, this.left, this.panelTop, this.contentWidth, this.panelBottom - this.panelTop);

		int logoY = 6;
		int textX = this.left;
		Identifier logo = ModIcons.get(this.config);
		if (logo != null) {
			TabbyGui.image(graphics, logo, this.left, logoY, LOGO_SIZE, -1);
			textX += LOGO_SIZE + 6;
		}
		int textWidth = this.left + this.contentWidth - textX - (this.searchBox != null ? this.searchBox.getWidth() + 8 : 0);
		graphics.text(this.font, TabbyGui.clip(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), textWidth),
			textX, logoY + 3, TabbyLibConfig.accentColor(), true);
		boolean showStatus = this.statusMessage != null && Util.getMillis() < this.statusUntil;
		Component subtitle = showStatus ? this.statusMessage : Component.literal(TabbyGui.version(this.config.getVersion()));
		graphics.text(this.font, TabbyGui.clip(this.font, subtitle, textWidth), textX, logoY + 14, showStatus ? 0xFFFFFFFF : 0xFF808080, false);

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
}
