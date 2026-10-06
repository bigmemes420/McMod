package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.module.FlightModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.SpeedModule;
import com.example.client.widget.CapsuleButton;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.LevelCapsuleButton;
import com.example.client.widget.ToggleCapsuleButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExampleMenuScreen extends Screen {
	private enum Tab {
		GENERAL,
		VISUALS,
		MOVEMENT,
		MISC
	}

	private static final int TOP_BAR_HEIGHT = 28;
	private static final int TAB_WIDTH = 78;
	private static final int TAB_HEIGHT = 20;
	private static final int CLOSE_WIDTH = 64;
	private static final int MENU_WIDTH = 64;
	private static final int CLOSE_HEIGHT = 20;
	private static final int CAPSULE_WIDTH = 220;
	private static final int CAPSULE_HEIGHT = 24;
	private static final int CONTENT_TOP = 56;
	private static final int CONTENT_LEFT = 24;
	private static final int CAPSULE_GAP = 8;

	/** Remembers the last top tab across menu open/close. */
	private static Tab lastSelectedTab = Tab.GENERAL;

	private Tab selectedTab = lastSelectedTab;
	private boolean colorMenuOpen;

	public ExampleMenuScreen() {
		super(Component.translatable("screen.modid.menu.title"));
	}

	@Override
	protected void init() {
		rebuildMenu();
	}

	private void rebuildMenu() {
		this.clearWidgets();

		int tabY = (TOP_BAR_HEIGHT - TAB_HEIGHT) / 2;
		int tabX = 8;
		addTab(tabX, tabY, Tab.GENERAL, "screen.modid.menu.tab.general");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.VISUALS, "screen.modid.menu.tab.visuals");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.MOVEMENT, "screen.modid.menu.tab.movement");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.MISC, "screen.modid.menu.tab.misc");

		int closeX = this.width - CLOSE_WIDTH - 8;
		int menuX = closeX - MENU_WIDTH - 6;
		int btnY = (TOP_BAR_HEIGHT - CLOSE_HEIGHT) / 2;

		this.addRenderableWidget(new FlatMenuButton(
				menuX,
				btnY,
				MENU_WIDTH,
				CLOSE_HEIGHT,
				Component.translatable("screen.modid.menu.colors"),
				this.colorMenuOpen,
				button -> {
					this.colorMenuOpen = !this.colorMenuOpen;
					rebuildMenu();
				}
		));

		this.addRenderableWidget(new FlatMenuButton(
				closeX,
				btnY,
				CLOSE_WIDTH,
				CLOSE_HEIGHT,
				Component.translatable("screen.modid.menu.close"),
				button -> this.onClose()
		));

		if (this.colorMenuOpen) {
			addColorMenuContent();
		} else {
			addContentButtons();
		}
	}

	private void addTab(int x, int y, Tab tab, String translationKey) {
		boolean selected = !this.colorMenuOpen && this.selectedTab == tab;
		this.addRenderableWidget(new FlatMenuButton(
				x,
				y,
				TAB_WIDTH,
				TAB_HEIGHT,
				Component.translatable(translationKey),
				selected,
				button -> {
					this.colorMenuOpen = false;
					if (this.selectedTab != tab) {
						this.selectedTab = tab;
						lastSelectedTab = tab;
					}
					rebuildMenu();
				}
		));
	}

	private void addContentButtons() {
		switch (this.selectedTab) {
			case MOVEMENT -> addMovementContent();
			case VISUALS -> addVisualsContent();
			case MISC -> addMiscContent();
			case GENERAL -> addGeneralContent();
		}
	}

	private void addGeneralContent() {
		String[] keys = {
				"screen.modid.menu.general.option1",
				"screen.modid.menu.general.option2",
				"screen.modid.menu.general.option3"
		};
		int y = CONTENT_TOP;
		for (String key : keys) {
			this.addRenderableWidget(new CapsuleButton(
					CONTENT_LEFT,
					y,
					CAPSULE_WIDTH,
					CAPSULE_HEIGHT,
					Component.translatable(key),
					button -> {
					}
			));
			y += CAPSULE_HEIGHT + CAPSULE_GAP;
		}
	}

	private void addVisualsContent() {
		int y = CONTENT_TOP;
		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.nametags"),
				NametagsModule.isEnabled(),
				NametagsModule.getScale(),
				NametagsModule.MIN_SCALE,
				NametagsModule.MAX_SCALE,
				NametagsModule::setEnabled,
				NametagsModule::setScale
		));
	}

	private void addMiscContent() {
		int y = CONTENT_TOP;
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.misc.notifications"),
				NotificationsModule.isEnabled(),
				(button, enabled) -> NotificationsModule.setEnabled(enabled)
		));
	}

	private void addMovementContent() {
		int y = CONTENT_TOP;
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.flight"),
				FlightModule.isEnabled(),
				(button, enabled) -> FlightModule.setEnabled(enabled)
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.speed"),
				SpeedModule.isEnabled(),
				SpeedModule.getSpeedLevel(),
				SpeedModule.MIN_LEVEL,
				SpeedModule.MAX_LEVEL,
				SpeedModule::setEnabled,
				SpeedModule::setSpeedLevel
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.nofall"),
				NoFallModule.isEnabled(),
				(button, enabled) -> NoFallModule.setEnabled(enabled)
		));
	}

	private void addColorMenuContent() {
		MenuTheme theme = MenuTheme.get();
		int y = CONTENT_TOP;
		int col = 0;
		int colWidth = CAPSULE_WIDTH + 16;
		int maxCols = Math.max(1, (this.width - CONTENT_LEFT * 2) / colWidth);
		var entries = theme.asMap();

		for (var entry : entries.entrySet()) {
			String key = entry.getKey();
			int color = entry.getValue();
			int x = CONTENT_LEFT + col * colWidth;
			String label = MenuTheme.displayName(key) + " #" + MenuTheme.toHex(color);
			this.addRenderableWidget(new CapsuleButton(
					x,
					y,
					CAPSULE_WIDTH,
					CAPSULE_HEIGHT,
					Component.literal(label),
					button -> this.minecraft.gui.setScreen(new ColorPickerScreen(this, key, color))
			));
			col++;
			if (col >= maxCols) {
				col = 0;
				y += CAPSULE_HEIGHT + CAPSULE_GAP;
			}
		}

		if (col != 0) {
			y += CAPSULE_HEIGHT + CAPSULE_GAP;
		}

		this.addRenderableWidget(new CapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.colors.reset"),
				button -> {
					MenuTheme.get().resetDefaults();
					rebuildMenu();
				}
		));
	}

	/** Rebuild widgets after returning from the color picker (theme may have changed). */
	public void refreshColorMenu() {
		if (this.colorMenuOpen) {
			rebuildMenu();
		}
	}

	/**
	 * Draw the top bar in the background stratum so tab/close/menu widgets (next stratum)
	 * render above the bar fill and any menu dimming/blur.
	 */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, theme.topBar);
		graphics.horizontalLine(0, this.width - 1, TOP_BAR_HEIGHT, theme.topBarLine);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();

		graphics.text(this.font, this.title, CONTENT_LEFT, TOP_BAR_HEIGHT + 8, theme.title, true);

		Component panel;
		if (this.colorMenuOpen) {
			panel = Component.translatable("screen.modid.menu.colors.panel");
		} else {
			String panelKey = switch (this.selectedTab) {
				case GENERAL -> "screen.modid.menu.tab.general";
				case VISUALS -> "screen.modid.menu.tab.visuals";
				case MOVEMENT -> "screen.modid.menu.tab.movement";
				case MISC -> "screen.modid.menu.tab.misc";
			};
			panel = Component.translatable("screen.modid.menu.panel", Component.translatable(panelKey));
		}
		graphics.text(this.font, panel, CONTENT_LEFT, CONTENT_TOP - 14, theme.panelHint, false);
	}
}
