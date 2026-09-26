package me.avie29.tabbylib.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.avie29.tabbylib.TabbyLibConfig;
import me.avie29.tabbylib.api.ActionEntry;
import me.avie29.tabbylib.api.ConfigCategory;
import me.avie29.tabbylib.api.ConfigEntry;
import me.avie29.tabbylib.api.LabelEntry;
import me.avie29.tabbylib.api.OptionGroup;
import me.avie29.tabbylib.api.TabbyConfig;
import me.avie29.tabbylib.api.TabbyLibApi;
import me.avie29.tabbylib.api.option.Option;
import me.avie29.tabbylib.client.gui.entry.OptionControls;
import me.avie29.tabbylib.client.gui.widget.FlatTab;
import me.avie29.tabbylib.client.gui.widget.TabbyButton;
import me.avie29.tabbylib.compat.McCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The TabbyLib config screen: mod list on the left, the options of the selected mod on the right.
 */
public class TabbyConfigScreen extends Screen {
	private static final int MARGIN = 8;
	private static final int GAP = 4;
	private static final int TOP = 24;
	private static final int FOOTER = 30;
	/** Wider screens get empty space at the sides, otherwise labels and controls drift too far apart. */
	private static final int MAX_WIDTH = 640;

	private final @Nullable Screen parent;
	private final List<TabbyConfig> configs;
	private @Nullable TabbyConfig selected;
	private @Nullable ConfigCategory selectedCategory;
	private String search = "";

	private @Nullable ModListWidget modList;
	private @Nullable OptionListWidget optionList;
	private @Nullable EditBox searchBox;
	private final List<FlatTab> tabs = new ArrayList<>();
	private OptionControls.@Nullable KeyBindControl capturingKey;
	private String visibilitySignature = "";

	private double restoreScroll;
	private @Nullable Component statusMessage;
	private long statusUntil;

	// Layout, computed in init()
	private int listX;
	private int listWidth;
	private int panelY;
	private int panelBottom;
	private int contentX;
	private int contentWidth;

	public TabbyConfigScreen(@Nullable Screen parent, @Nullable String modId) {
		super(Component.translatable("tabbylib.screen.title"));
		this.parent = parent;
		this.configs = TabbyLibApi.getConfigs();
		if (modId != null) {
			this.selected = TabbyLibApi.getConfig(modId);
		}
		if (this.selected == null && !this.configs.isEmpty()) {
			this.selected = this.configs.getFirst();
		}
	}

	// ---------------------------------------------------------------- layout

	@Override
	protected void init() {
		TabbyLibConfig.ListWidth widthSetting = TabbyLibConfig.LIST_WIDTH.getPending();
		int usableWidth = Math.min(this.width - MARGIN * 2, MAX_WIDTH);
		this.listX = (this.width - usableWidth) / 2;
		this.listWidth = Mth.clamp(usableWidth / 4, widthSetting.min, widthSetting.max);
		this.panelY = TOP;
		this.panelBottom = this.height - FOOTER;
		this.contentX = this.listX + this.listWidth + GAP;
		this.contentWidth = this.listX + usableWidth - this.contentX;

		this.modList = new ModListWidget(this.minecraft, this.listX + 2, this.panelY + 3, this.listWidth - 4,
			this.panelBottom - this.panelY - 6, this.configs, this::selectConfig);
		this.addRenderableWidget(this.modList);

		int searchWidth = Math.min(130, this.contentWidth / 3);
		this.searchBox = new EditBox(this.font, this.contentX + this.contentWidth - searchWidth - 6, this.panelY + 5, searchWidth, 16,
			Component.translatable("tabbylib.search"));
		this.searchBox.setHint(Component.translatable("tabbylib.search").withStyle(EditBox.SEARCH_HINT_STYLE));
		this.searchBox.setValue(this.search);
		this.searchBox.setResponder(text -> {
			this.search = text;
			this.rebuildTabs();
			this.rebuildOptions();
		});
		this.addRenderableWidget(this.searchBox);

		this.optionList = new OptionListWidget(this.minecraft, this, this.contentX + 2, 0, this.contentWidth - 4, 10);
		this.addRenderableWidget(this.optionList);

		// Footer buttons
		int buttonWidth = Math.min(100, (this.width - MARGIN * 2 - GAP * 3) / 4);
		int totalWidth = buttonWidth * 4 + GAP * 3;
		int x = (this.width - totalWidth) / 2;
		int y = this.height - FOOTER + 5;
		TabbyButton reset = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.reset"), this::resetSelected);
		reset.setTooltip(Tooltip.create(Component.translatable("tabbylib.button.reset.tooltip")));
		TabbyButton discard = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.discard"), this::discardAll);
		TabbyButton save = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.save"), this::saveAll);
		TabbyButton done = new TabbyButton(buttonWidth, 20, Component.translatable("tabbylib.button.done"), () -> {
			this.saveAll();
			this.closeScreen();
		});
		for (TabbyButton button : List.of(reset, discard, save, done)) {
			button.setPosition(x, y);
			x += buttonWidth + GAP;
			this.addRenderableWidget(button);
		}

		if (this.selected != null) {
			this.modList.select(this.selected);
		}
		this.rebuildTabs();
		this.rebuildOptions();
		this.optionList.setScrollAmount(this.restoreScroll);
		this.restoreScroll = 0;
	}

	private void selectConfig(TabbyConfig config) {
		if (this.selected != config) {
			this.selected = config;
			this.selectedCategory = null;
			this.rebuildTabs();
			this.rebuildOptions();
			if (this.optionList != null) {
				this.optionList.setScrollAmount(0);
			}
		}
	}

	private void rebuildTabs() {
		this.tabs.forEach(this::removeWidget);
		this.tabs.clear();
		if (this.selected == null) {
			return;
		}
		List<ConfigCategory> categories = this.selected.getCategories();
		if (this.selectedCategory == null || !categories.contains(this.selectedCategory)) {
			this.selectedCategory = categories.getFirst();
		}
		if (categories.size() > 1 && this.search.isBlank()) {
			int x = this.contentX + 6;
			int available = this.contentWidth - 12;
			int maxTabWidth = available / categories.size();
			for (ConfigCategory category : categories) {
				int tabWidth = Math.min(maxTabWidth, this.font.width(category.getName()) + 12);
				FlatTab tab = new FlatTab(tabWidth, 14, category.getName(), () -> this.selectedCategory == category, () -> {
					this.selectedCategory = category;
					this.rebuildOptions();
					if (this.optionList != null) {
						this.optionList.setScrollAmount(0);
					}
				});
				tab.setPosition(x, this.panelY + 24);
				x += tabWidth + 2;
				this.tabs.add(this.addRenderableWidget(tab));
			}
		}
	}

	private int optionListTop() {
		return this.panelY + (this.tabs.isEmpty() ? 25 : 41);
	}

	/** Recreates the option rows, e.g. after changing category, search or collapsing a group. */
	public void rebuildOptions() {
		if (this.optionList == null) {
			return;
		}
		double scroll = this.optionList.scrollAmount();
		int top = this.optionListTop();
		this.optionList.updateSizeAndPosition(this.contentWidth - 4, this.panelBottom - top - 3, this.contentX + 2, top);
		this.optionList.clear();
		this.visibilitySignature = this.computeVisibilitySignature();

		if (this.selected == null) {
			this.optionList.addText(Component.translatable("tabbylib.no_mods"), false);
			return;
		}

		String query = this.search.trim().toLowerCase(Locale.ROOT);
		if (!query.isEmpty()) {
			this.addSearchResults(query);
		} else if (this.selectedCategory != null) {
			this.addEntries(this.selectedCategory.getEntries(), false);
		}
		this.optionList.setScrollAmount(scroll);
	}

	private void addEntries(List<ConfigEntry> entries, boolean indented) {
		for (ConfigEntry entry : entries) {
			switch (entry) {
				case Option<?> option -> {
					if (option.isVisible()) {
						this.optionList.addOption(option, indented);
					}
				}
				case OptionGroup group -> {
					this.optionList.addGroup(group);
					if (!group.isCollapsed()) {
						this.addEntries(group.getEntries(), true);
					}
				}
				case LabelEntry label -> this.optionList.addText(label.text(), indented);
				case ActionEntry action -> this.optionList.addAction(action, indented);
				default -> {
				}
			}
		}
	}

	private void addSearchResults(String query) {
		boolean any = false;
		for (ConfigCategory category : this.selected.getCategories()) {
			boolean headerAdded = false;
			for (Option<?> option : category.getOptions()) {
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

	private static boolean matches(Option<?> option, String query) {
		if (option.getName().getString().toLowerCase(Locale.ROOT).contains(query)
			|| option.getKey().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		Component tooltip = option.getTooltip();
		return tooltip != null && tooltip.getString().toLowerCase(Locale.ROOT).contains(query);
	}

	/** Which options are visible right now. When it changes (visibleWhen) the list is rebuilt. */
	private String computeVisibilitySignature() {
		if (this.selected == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Option<?> option : this.selected.getOptions()) {
			builder.append(option.isVisible() ? '1' : '0');
		}
		return builder.toString();
	}

	@Override
	public void tick() {
		if (!this.computeVisibilitySignature().equals(this.visibilitySignature)) {
			this.rebuildOptions();
		}
	}

	// ---------------------------------------------------------------- actions

	private void resetSelected() {
		if (this.selected != null) {
			this.selected.resetPendingToDefaults();
			this.refreshAfterChange();
		}
	}

	private void discardAll() {
		this.configs.forEach(TabbyConfig::discard);
		this.refreshAfterChange();
		this.showStatus(Component.translatable("tabbylib.status.discarded"));
	}

	private void saveAll() {
		boolean changed = false;
		boolean restart = false;
		for (TabbyConfig config : this.configs) {
			if (config.hasChanges()) {
				changed = true;
				restart |= config.commit();
			} else {
				config.discard();
			}
		}
		this.refreshAfterChange();
		if (restart) {
			this.showStatus(Component.translatable("tabbylib.status.restart").withStyle(ChatFormatting.RED));
		} else if (changed) {
			this.showStatus(Component.translatable("tabbylib.status.saved").withStyle(ChatFormatting.GREEN));
		}
	}

	private void refreshAfterChange() {
		// Settings like the list width only apply after a re-layout, which also refreshes all controls
		this.rememberScroll();
		this.rebuildWidgets();
	}

	private void rememberScroll() {
		this.restoreScroll = this.optionList != null ? this.optionList.scrollAmount() : 0;
	}

	private void showStatus(Component message) {
		this.statusMessage = message;
		this.statusUntil = Util.getMillis() + 3000;
	}

	public void openSubScreen(Screen screen) {
		this.rememberScroll();
		McCompat.setScreen(screen);
	}

	private boolean hasUnsavedChanges() {
		for (TabbyConfig config : this.configs) {
			if (config.hasChanges()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onClose() {
		if (this.hasUnsavedChanges() && TabbyLibConfig.CONFIRM_UNSAVED.get()) {
			McCompat.setScreen(new ConfirmScreen(save -> {
				if (save) {
					this.saveAll();
				} else {
					this.configs.forEach(TabbyConfig::discard);
				}
				this.closeScreen();
			}, Component.translatable("tabbylib.unsaved.title"), Component.translatable("tabbylib.unsaved.message"),
				Component.translatable("tabbylib.button.save"), Component.translatable("tabbylib.button.discard")));
			return;
		}
		this.configs.forEach(TabbyConfig::discard);
		this.closeScreen();
	}

	private void closeScreen() {
		McCompat.setScreen(this.parent);
	}

	// ---------------------------------------------------------------- key binding capture

	public void startKeyCapture(OptionControls.KeyBindControl control) {
		OptionControls.KeyBindControl previous = this.capturingKey;
		this.capturingKey = control;
		if (previous != null && previous != control) {
			previous.refresh();
		}
	}

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
		// Ctrl + F focuses the search
		if (event.input() == InputConstants.KEY_F && event.hasControlDown() && this.searchBox != null) {
			this.setFocused(this.searchBox);
			return true;
		}
		// Ctrl + S saves
		if (event.input() == InputConstants.KEY_S && event.hasControlDown()) {
			this.saveAll();
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

	// ---------------------------------------------------------------- rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Panel.draw(graphics, this.listX, this.panelY, this.listWidth, this.panelBottom - this.panelY);
		Panel.draw(graphics, this.contentX, this.panelY, this.contentWidth, this.panelBottom - this.panelY);

		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

		if (this.selected != null) {
			int accent = TabbyLibConfig.accentColor();
			int titleMaxWidth = this.contentWidth - (this.searchBox != null ? this.searchBox.getWidth() : 0) - 20;
			graphics.text(this.font, OptionListWidget.clip(this.font, this.selected.getDisplayName().copy().withStyle(ChatFormatting.BOLD), titleMaxWidth),
				this.contentX + 7, this.panelY + 9, accent, true);
			int lineY = this.optionListTop() - 2;
			graphics.fill(this.contentX + 4, lineY, this.contentX + this.contentWidth - 4, lineY + 1, 0x40FFFFFF);
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);

		if (this.statusMessage != null) {
			if (Util.getMillis() < this.statusUntil) {
				int textWidth = this.font.width(this.statusMessage);
				int x = this.contentX + this.contentWidth - textWidth;
				graphics.text(this.font, this.statusMessage, x, 8, 0xFFFFFFFF, true);
			} else {
				this.statusMessage = null;
			}
		}
	}
}
