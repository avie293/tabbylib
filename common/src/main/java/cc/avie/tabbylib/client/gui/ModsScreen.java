package cc.avie.tabbylib.client.gui;

import cc.avie.tabbylib.api.TabbyConfig;
import cc.avie.tabbylib.api.TabbyLibApi;
import cc.avie.tabbylib.client.gui.widget.TabbyButton;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class ModsScreen extends Screen {
	private static final int GRID_TOP = 52;
	private static final int FOOTER = 34;

	private final @Nullable Screen parent;
	private final List<TabbyConfig> configs;
	private String search = "";
	private @Nullable ModGrid grid;
	private @Nullable EditBox searchBox;

	public ModsScreen(@Nullable Screen parent) {
		super(Component.translatable("tabbylib.screen.title"));
		this.parent = parent;
		this.configs = TabbyLibApi.getConfigs();
	}

	@Override
	protected void init() {
		int searchWidth = Math.min(200, this.width - 40);
		this.searchBox = new EditBox(this.font, (this.width - searchWidth) / 2, 26, searchWidth, 18, Component.translatable("tabbylib.search.mods"));
		this.searchBox.setHint(Component.translatable("tabbylib.search.mods").withStyle(EditBox.SEARCH_HINT_STYLE));
		this.searchBox.setValue(this.search);
		this.searchBox.setResponder(text -> {
			this.search = text;
			this.updateGrid();
		});
		this.addRenderableWidget(this.searchBox);

		this.grid = new ModGrid(this.minecraft, this.width, this.height - GRID_TOP - FOOTER, GRID_TOP, this::open);
		this.addRenderableWidget(this.grid);
		this.updateGrid();

		TabbyButton done = new TabbyButton(150, 20, Component.translatable("gui.done"), this::onClose);
		done.setPosition((this.width - 150) / 2, this.height - 27);
		this.addRenderableWidget(done);
	}

	private void updateGrid() {
		if (this.grid == null) {
			return;
		}
		String query = this.search.trim().toLowerCase(Locale.ROOT);
		this.grid.setConfigs(this.configs.stream()
			.filter(config -> query.isEmpty()
				|| config.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(query)
				|| config.getModId().toLowerCase(Locale.ROOT).contains(query))
			.toList());
	}

	private void open(TabbyConfig config) {
		Function<Screen, Screen> customScreen = config.getCustomScreen();
		this.minecraft.gui.setScreen(customScreen != null ? customScreen.apply(this) : new ModConfigScreen(this, config));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.input() == InputConstants.KEY_F && event.hasControlDown() && this.searchBox != null) {
			this.setFocused(this.searchBox);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (this.grid != null && this.grid.isEmpty()) {
			Component empty = Component.translatable(this.configs.isEmpty() ? "tabbylib.no_mods" : "tabbylib.search.no_mods");
			graphics.centeredText(this.font, empty, this.width / 2, GRID_TOP + 20, 0xFFA0A0A0);
		}
	}
}
