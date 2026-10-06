package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.FlightModule;
import com.example.client.module.FinderModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.PlayerOutlinesModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.VelocityModule;
import com.example.client.module.XRayModule;
import com.example.client.widget.CapsuleButton;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.LevelCapsuleButton;
import com.example.client.widget.ToggleCapsuleButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class ExampleMenuScreen extends Screen {
	private enum Tab {
		GENERAL,
		VISUALS,
		COMBAT,
		MOVEMENT,
		MISC
	}

	/** Vertical space above the top bar for the scaled title. */
	private static final int TITLE_BAND = 26;
	private static final int TOP_BAR_HEIGHT = 28;
	private static final int TAB_WIDTH = 70;
	private static final int TAB_HEIGHT = 20;
	private static final int CLOSE_WIDTH = 64;
	private static final int MENU_WIDTH = 64;
	private static final int CLOSE_HEIGHT = 20;
	private static final int CAPSULE_WIDTH = 220;
	private static final int CAPSULE_HEIGHT = 24;
	private static final int EDIT_WIDTH = 56;
	private static final int CONTENT_TOP = TITLE_BAND + TOP_BAR_HEIGHT + 28;
	private static final int CONTENT_LEFT = 24;
	private static final int CAPSULE_GAP = 8;
	private static final float TITLE_SCALE = 1.6F;

	/** Remembers the last top tab across menu open/close (also persisted). */
	private static Tab lastSelectedTab = Tab.GENERAL;

	private Tab selectedTab = lastSelectedTab;

	public static String getLastTabName() {
		return lastSelectedTab.name();
	}

	public static void loadLastTab(String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		try {
			lastSelectedTab = Tab.valueOf(name.trim());
		} catch (IllegalArgumentException ignored) {
			// keep default
		}
	}

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

		int tabY = TITLE_BAND + (TOP_BAR_HEIGHT - TAB_HEIGHT) / 2;
		int tabX = 8;
		addTab(tabX, tabY, Tab.GENERAL, "screen.modid.menu.tab.general");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.VISUALS, "screen.modid.menu.tab.visuals");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.COMBAT, "screen.modid.menu.tab.combat");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.MOVEMENT, "screen.modid.menu.tab.movement");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.MISC, "screen.modid.menu.tab.misc");

		int closeX = this.width - CLOSE_WIDTH - 8;
		int menuX = closeX - MENU_WIDTH - 6;
		int btnY = TITLE_BAND + (TOP_BAR_HEIGHT - CLOSE_HEIGHT) / 2;

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
						ModConfig.save();
					}
					rebuildMenu();
				}
		));
	}

	private void addContentButtons() {
		switch (this.selectedTab) {
			case MOVEMENT -> addMovementContent();
			case VISUALS -> addVisualsContent();
			case COMBAT -> addCombatContent();
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
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.player_outlines"),
				PlayerOutlinesModule.isEnabled(),
				(button, enabled) -> PlayerOutlinesModule.setEnabled(enabled)
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.fullbright"),
				FullbrightModule.isEnabled(),
				(button, enabled) -> FullbrightModule.setEnabled(enabled)
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.xray"),
				XRayModule.isEnabled(),
				XRayModule.getOpacity(),
				XRayModule.MIN_OPACITY,
				XRayModule.MAX_OPACITY,
				XRayModule::setEnabled,
				XRayModule::setOpacity
		));
		this.addRenderableWidget(new CapsuleButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 8,
				y,
				EDIT_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.xray.edit"),
				button -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new XRayBlocksScreen(this));
					}
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.finder"),
				FinderModule.isEnabled(),
				(button, enabled) -> FinderModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CapsuleButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 8,
				y,
				EDIT_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.finder.edit"),
				button -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new FinderBlocksScreen(this));
					}
				}
		));
	}

	private void addCombatContent() {
		int y = CONTENT_TOP;
		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.combat.autoclicker"),
				AutoClickerModule.isEnabled(),
				AutoClickerModule.getCps(),
				AutoClickerModule.MIN_CPS,
				AutoClickerModule.MAX_CPS,
				AutoClickerModule::setEnabled,
				AutoClickerModule::setCps
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.combat.velocity"),
				VelocityModule.isEnabled(),
				VelocityModule.getPercent(),
				VelocityModule.MIN_PERCENT,
				VelocityModule.MAX_PERCENT,
				VelocityModule::setEnabled,
				VelocityModule::setPercent
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		this.addRenderableWidget(new LevelCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.combat.reach"),
				ReachModule.isEnabled(),
				ReachModule.getBonus(),
				ReachModule.MIN_BONUS,
				ReachModule.MAX_BONUS,
				ReachModule::setEnabled,
				ReachModule::setBonus
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

	/** Insert (or rebound open key) also closes the menu while it is open. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ExampleModClient.matchesOpenMenuKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/**
	 * Draw the top bar in the background stratum so tab/close/menu widgets (next stratum)
	 * render above the bar fill and any menu dimming/blur.
	 */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		int barTop = TITLE_BAND;
		int barBottom = TITLE_BAND + TOP_BAR_HEIGHT;
		graphics.fill(0, barTop, this.width, barBottom, theme.topBar);
		graphics.horizontalLine(0, this.width - 1, barBottom, theme.topBarLine);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();

		// Title above the top bar at 60% larger font
		var pose = graphics.pose();
		pose.pushMatrix();
		float titleY = (TITLE_BAND - this.font.lineHeight * TITLE_SCALE) / 2.0F;
		pose.translate(CONTENT_LEFT, titleY);
		pose.scale(TITLE_SCALE);
		graphics.text(this.font, this.title, 0, 0, theme.title, true);
		pose.popMatrix();

		Component panel;
		if (this.colorMenuOpen) {
			panel = Component.translatable("screen.modid.menu.colors.panel");
		} else {
			String panelKey = switch (this.selectedTab) {
				case GENERAL -> "screen.modid.menu.tab.general";
				case VISUALS -> "screen.modid.menu.tab.visuals";
				case COMBAT -> "screen.modid.menu.tab.combat";
				case MOVEMENT -> "screen.modid.menu.tab.movement";
				case MISC -> "screen.modid.menu.tab.misc";
			};
			panel = Component.translatable("screen.modid.menu.panel", Component.translatable(panelKey));
		}
		graphics.text(this.font, panel, CONTENT_LEFT, CONTENT_TOP - 14, theme.panelHint, false);
	}
}
