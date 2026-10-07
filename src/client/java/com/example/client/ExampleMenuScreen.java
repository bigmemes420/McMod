package com.example.client;

import com.example.ExampleMod;
import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;
import com.example.client.module.AirPlaceModule;
import com.example.client.module.AimAssistModule;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.AutoSprintModule;
import com.example.client.module.AutoTotemModule;
import com.example.client.module.CriticalsModule;
import com.example.client.module.ElytraControlModule;
import com.example.client.module.FlightModule;
import com.example.client.module.FinderModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.HitboxesModule;
import com.example.client.module.JesusModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.InventoryMoveModule;
import com.example.client.module.MobEspModule;
import com.example.client.module.NoSlowModule;
import com.example.client.module.PlayerEspModule;
import com.example.client.module.RadarModule;
import com.example.client.module.CustomCrosshairModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SafeWalkModule;
import com.example.client.module.SneakModule;
import com.example.client.module.TowerModule;
import com.example.client.module.ScaffoldModule;
import com.example.client.module.ModuleKeybinds;
import com.example.client.module.FastPlaceModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.SpiderModule;
import com.example.client.module.StepModule;
import com.example.client.module.TriggerBotModule;
import com.example.client.module.VelocityModule;
import com.example.client.widget.CapsuleButton;
import com.example.client.widget.CogButton;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.LabeledSliderWidget;
import com.example.client.widget.ModeDropdownButton;
import com.example.client.widget.ColorSwatchButton;
import com.example.client.widget.ToggleCapsuleButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ExampleMenuScreen extends Screen {
	private enum Tab {
		GENERAL,
		PLAYER,
		VISUALS,
		COMBAT,
		WORLD,
		MOVEMENT,
		MISC
	}

	/** Vertical space above the top bar for the scaled title. */
	private static final int TITLE_BAND = 26;
	private static final int TOP_BAR_HEIGHT = 28;
	private static final int TAB_WIDTH = 64;
	private static final int TAB_HEIGHT = 20;
	private static final int CLOSE_WIDTH = 64;
	private static final int MENU_WIDTH = 64;
	private static final int CLOSE_HEIGHT = 20;
	/** Shorter module capsules; slightly taller than the old 24px bar. */
	private static final int CAPSULE_WIDTH = 152;
	private static final int CAPSULE_HEIGHT = 26;
	private static final int COG_SIZE = 26;
	private static final int EDIT_WIDTH = 48;
	private static final int MODE_WIDTH = 120;
	private static final int SETTINGS_WIDTH = 300;
	private static final int SLIDER_HEIGHT = 22;
	private static final int CONTENT_TOP = TITLE_BAND + TOP_BAR_HEIGHT + 28;
	private static final int CONTENT_LEFT = 24;
	private static final int CAPSULE_GAP = 8;
	private static final int SETTINGS_GAP = 4;
	private static final float TITLE_SCALE = 1.6F;
	/** Logo drawn top-left; title shifts right of it. */
	private static final int LOGO_SIZE = 24;
	private static final int LOGO_TEX_SIZE = 128;
	private static final int LOGO_LEFT = 6;
	private static final int TITLE_LEFT = LOGO_LEFT + LOGO_SIZE + 8;
	private static final Identifier ROOTY_LOGO = ExampleMod.id("textures/gui/rooty_logo.png");

	/** Remembers the last top tab across menu open/close (also persisted). */
	private static Tab lastSelectedTab = Tab.GENERAL;

	private Tab selectedTab = lastSelectedTab;

	/** Which module settings panel is open ({@code null} = none). */
	private String openSettingsId;

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
		addTab(tabX, tabY, Tab.PLAYER, "screen.modid.menu.tab.player");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.VISUALS, "screen.modid.menu.tab.visuals");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.COMBAT, "screen.modid.menu.tab.combat");
		tabX += TAB_WIDTH + 6;
		addTab(tabX, tabY, Tab.WORLD, "screen.modid.menu.tab.world");
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
					this.openSettingsId = null;
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
			case WORLD -> addWorldContent();
			case MISC -> addMiscContent();
			case PLAYER -> addPlayerContent();
			case GENERAL -> addGeneralContent();
		}
	}

	private void toggleSettings(String id) {
		this.openSettingsId = id.equals(this.openSettingsId) ? null : id;
		rebuildMenu();
	}

	private boolean settingsOpen(String id) {
		return id.equals(this.openSettingsId);
	}

	/** Toggle capsule (+ optional cog). Returns Y after the row. */
	private int addToggleModule(
			int y,
			String settingsId,
			String moduleId,
			Component label,
			boolean enabled,
			ToggleCapsuleButton.OnToggle onToggle
	) {
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				label,
				enabled,
				moduleId,
				onToggle
		));
		if (settingsId != null) {
			this.addRenderableWidget(new CogButton(
					CONTENT_LEFT + CAPSULE_WIDTH + 6,
					y,
					COG_SIZE,
					settingsOpen(settingsId),
					button -> toggleSettings(settingsId)
			));
		}
		return y + CAPSULE_HEIGHT + CAPSULE_GAP;
	}

	private void addLabeledSlider(
			int x,
			int y,
			int width,
			Component label,
			float value,
			float min,
			float max,
			LabeledSliderWidget.OnLevelChange onChange
	) {
		addLabeledSlider(x, y, width, label, value, min, max, onChange, null);
	}

	private void addLabeledSlider(
			int x,
			int y,
			int width,
			Component label,
			float value,
			float min,
			float max,
			LabeledSliderWidget.OnLevelChange onChange,
			Runnable onRelease
	) {
		this.addRenderableWidget(new LabeledSliderWidget(
				x, y, width, SLIDER_HEIGHT, label, value, min, max, onChange, onRelease
		));
	}

	private void addColorSettingRow(int y, String label, int color, java.util.function.Consumer<Integer> onApply) {
		int swatch = 28;
		int gap = 8;
		this.addRenderableWidget(new FlatMenuButton(
				CONTENT_LEFT,
				y,
				SETTINGS_WIDTH - swatch - gap,
				CAPSULE_HEIGHT,
				Component.literal(label + " #" + MenuTheme.toHex(color)),
				button -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new ColorPickerScreen(this, label, color, onApply));
					}
				}
		));
		this.addRenderableWidget(new ColorSwatchButton(
				CONTENT_LEFT + SETTINGS_WIDTH - swatch,
				y,
				swatch,
				CAPSULE_HEIGHT,
				color,
				() -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new ColorPickerScreen(this, label, color, onApply));
					}
				}
		));
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

		y = addToggleModule(
				y,
				"nametags",
				"nametags",
				Component.translatable("screen.modid.menu.visuals.nametags"),
				NametagsModule.isEnabled(),
				(button, enabled) -> NametagsModule.setEnabled(enabled)
		);
		if (settingsOpen("nametags")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.visuals.nametags.scale"),
					NametagsModule.getScale(),
					NametagsModule.MIN_SCALE,
					NametagsModule.MAX_SCALE,
					NametagsModule::setScale
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		// Player ESP: capsule + cog + mode dropdown (Outline / 2D / 3D)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.player_esp"),
				PlayerEspModule.isEnabled(),
				"player_esp",
				(button, enabled) -> PlayerEspModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CogButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				COG_SIZE,
				settingsOpen("player_esp"),
				button -> toggleSettings("player_esp")
		));
		int playerModeIndex = switch (PlayerEspModule.getMode()) {
			case BOX_2D -> 1;
			case BOX_3D -> 2;
			default -> 0;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6 + COG_SIZE + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.visuals.player_esp.mode.outline"),
					Component.translatable("screen.modid.menu.visuals.player_esp.mode.box_2d"),
					Component.translatable("screen.modid.menu.visuals.player_esp.mode.box_3d")
				},
				playerModeIndex,
				index -> {
					PlayerEspModule.Mode mode = switch (index) {
						case 1 -> PlayerEspModule.Mode.BOX_2D;
						case 2 -> PlayerEspModule.Mode.BOX_3D;
						default -> PlayerEspModule.Mode.OUTLINE;
					};
					PlayerEspModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("player_esp")) {
			addColorSettingRow(y, "Color", PlayerEspModule.getColor(), c -> {
				PlayerEspModule.setColor(c);
				rebuildMenu();
			});
			y += CAPSULE_HEIGHT + SETTINGS_GAP;
			this.addRenderableWidget(new ToggleCapsuleButton(
					CONTENT_LEFT,
					y,
					SETTINGS_WIDTH,
					CAPSULE_HEIGHT,
					Component.translatable("screen.modid.menu.visuals.player_esp.outline_boxes"),
					PlayerEspModule.isOutlineBoxes(),
					(button, enabled) -> PlayerEspModule.setOutlineBoxes(enabled)
			));
			y += CAPSULE_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		// Mob ESP: capsule + Edit + mode dropdown (Outline / 2D / 3D)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.mob_esp"),
				MobEspModule.isEnabled(),
				"mob_esp",
				(button, enabled) -> MobEspModule.setEnabled(enabled)
		));
		int mobAfterCapsule = CONTENT_LEFT + CAPSULE_WIDTH + 6;
		this.addRenderableWidget(new FlatMenuButton(
				mobAfterCapsule,
				y,
				EDIT_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.mob_esp.edit"),
				button -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new MobEspMobsScreen(this));
					}
				}
		));
		int mobModeIndex = switch (MobEspModule.getMode()) {
			case BOX_2D -> 1;
			case BOX_3D -> 2;
			default -> 0;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				mobAfterCapsule + EDIT_WIDTH + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.visuals.mob_esp.mode.outline"),
					Component.translatable("screen.modid.menu.visuals.mob_esp.mode.box_2d"),
					Component.translatable("screen.modid.menu.visuals.mob_esp.mode.box_3d")
				},
				mobModeIndex,
				index -> {
					MobEspModule.Mode mode = switch (index) {
						case 1 -> MobEspModule.Mode.BOX_2D;
						case 2 -> MobEspModule.Mode.BOX_3D;
						default -> MobEspModule.Mode.OUTLINE;
					};
					MobEspModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		y = addToggleModule(
				y,
				null,
				"fullbright",
				Component.translatable("screen.modid.menu.visuals.fullbright"),
				FullbrightModule.isEnabled(),
				(button, enabled) -> FullbrightModule.setEnabled(enabled)
		);


		// Finder: capsule + cog + Edit + mode dropdown (Edit next to cog; dropdown right of Edit)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.finder"),
				FinderModule.isEnabled(),
				"finder",
				(button, enabled) -> FinderModule.setEnabled(enabled)
		));
		int finderCogX = CONTENT_LEFT + CAPSULE_WIDTH + 6;
		int finderEditX = finderCogX + COG_SIZE + 6;
		int finderDropdownX = finderEditX + EDIT_WIDTH + 6;
		this.addRenderableWidget(new CogButton(
				finderCogX,
				y,
				COG_SIZE,
				settingsOpen("finder"),
				button -> toggleSettings("finder")
		));
		this.addRenderableWidget(new FlatMenuButton(
				finderEditX,
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
		int finderModeIndex = switch (FinderModule.getMode()) {
			case FILLED -> 1;
			case COMBINED_FILL -> 2;
			default -> 0;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				finderDropdownX,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.visuals.finder.mode.outline"),
					Component.translatable("screen.modid.menu.visuals.finder.mode.filled"),
					Component.translatable("screen.modid.menu.visuals.finder.mode.combined_fill")
				},
				finderModeIndex,
				index -> {
					FinderModule.RenderMode mode = switch (index) {
						case 1 -> FinderModule.RenderMode.FILLED;
						case 2 -> FinderModule.RenderMode.COMBINED_FILL;
						default -> FinderModule.RenderMode.OUTLINE;
					};
					FinderModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("finder")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.visuals.finder.opacity"),
					FinderModule.getOpacity(),
					FinderModule.MIN_OPACITY,
					FinderModule.MAX_OPACITY,
					FinderModule::setOpacity
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP;
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.visuals.finder.thickness"),
					FinderModule.getOutlineThickness(),
					FinderModule.MIN_OUTLINE_THICKNESS,
					FinderModule.MAX_OUTLINE_THICKNESS,
					FinderModule::setOutlineThickness
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP;
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.visuals.finder.distance"),
					FinderModule.getDistance(),
					FinderModule.MIN_DISTANCE,
					FinderModule.MAX_DISTANCE,
					FinderModule::setDistance
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		// Radar: capsule + cog + mode dropdown (Mobs / Players / Both)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.radar"),
				RadarModule.isEnabled(),
				"radar",
				(button, enabled) -> RadarModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CogButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				COG_SIZE,
				settingsOpen("radar"),
				button -> toggleSettings("radar")
		));
		int radarModeIndex = switch (RadarModule.getMode()) {
			case MOBS -> 0;
			case PLAYERS -> 1;
			default -> 2;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6 + COG_SIZE + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.visuals.radar.mode.mobs"),
					Component.translatable("screen.modid.menu.visuals.radar.mode.players"),
					Component.translatable("screen.modid.menu.visuals.radar.mode.both")
				},
				radarModeIndex,
				index -> {
					RadarModule.TargetMode mode = switch (index) {
						case 0 -> RadarModule.TargetMode.MOBS;
						case 1 -> RadarModule.TargetMode.PLAYERS;
						default -> RadarModule.TargetMode.BOTH;
					};
					RadarModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("radar")) {
			int radarShapeIndex = switch (RadarModule.getShape()) {
				case CIRCLE -> 1;
				case TRIANGLE -> 2;
				case STAR -> 3;
				default -> 0;
			};
			this.addRenderableWidget(new ModeDropdownButton(
					CONTENT_LEFT,
					y,
					SETTINGS_WIDTH,
					CAPSULE_HEIGHT,
					new Component[] {
						Component.translatable("screen.modid.menu.visuals.radar.shape.square"),
						Component.translatable("screen.modid.menu.visuals.radar.shape.circle"),
						Component.translatable("screen.modid.menu.visuals.radar.shape.triangle"),
						Component.translatable("screen.modid.menu.visuals.radar.shape.star")
					},
					radarShapeIndex,
					index -> {
						RadarModule.Shape shape = switch (index) {
							case 1 -> RadarModule.Shape.CIRCLE;
							case 2 -> RadarModule.Shape.TRIANGLE;
							case 3 -> RadarModule.Shape.STAR;
							default -> RadarModule.Shape.SQUARE;
						};
						RadarModule.setShape(shape);
						rebuildMenu();
					}
			));
			y += CAPSULE_HEIGHT + SETTINGS_GAP;
			this.addRenderableWidget(new ToggleCapsuleButton(
					CONTENT_LEFT,
					y,
					SETTINGS_WIDTH,
					CAPSULE_HEIGHT,
					Component.translatable("screen.modid.menu.visuals.radar.show_height"),
					RadarModule.isShowHeight(),
					(button, enabled) -> RadarModule.setShowHeight(enabled)
			));
			y += CAPSULE_HEIGHT + SETTINGS_GAP;
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.visuals.radar.range"),
					RadarModule.getRange(),
					RadarModule.MIN_RANGE,
					RadarModule.MAX_RANGE,
					RadarModule::setRange
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		// Custom Crosshair: capsule + Edit + cog settings (color / rotate / spin)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.custom_crosshair"),
				CustomCrosshairModule.isEnabled(),
				"custom_crosshair",
				(button, enabled) -> CustomCrosshairModule.setEnabled(enabled)
		));
		int crossEditX = CONTENT_LEFT + CAPSULE_WIDTH + 6;
		int crossCogX = crossEditX + EDIT_WIDTH + 6;
		this.addRenderableWidget(new FlatMenuButton(
				crossEditX,
				y,
				EDIT_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.custom_crosshair.edit"),
				button -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new CrosshairEditorScreen(this));
					}
				}
		));
		this.addRenderableWidget(new CogButton(
				crossCogX,
				y,
				COG_SIZE,
				settingsOpen("custom_crosshair"),
				button -> toggleSettings("custom_crosshair")
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("custom_crosshair")) {
			addColorSettingRow(y, "Color", CustomCrosshairModule.getColor(), c -> {
				CustomCrosshairModule.setColor(c);
				rebuildMenu();
			});
			y += CAPSULE_HEIGHT + SETTINGS_GAP;
			this.addRenderableWidget(new ToggleCapsuleButton(
					CONTENT_LEFT,
					y,
					SETTINGS_WIDTH,
					CAPSULE_HEIGHT,
					Component.translatable("screen.modid.menu.visuals.custom_crosshair.rotate"),
					CustomCrosshairModule.isRotate(),
					(button, enabled) -> {
						CustomCrosshairModule.setRotate(enabled);
						rebuildMenu();
					}
			));
			y += CAPSULE_HEIGHT + SETTINGS_GAP;
			if (CustomCrosshairModule.isRotate()) {
				addLabeledSlider(
						CONTENT_LEFT, y, SETTINGS_WIDTH,
						Component.translatable("screen.modid.menu.visuals.custom_crosshair.spin_speed"),
						CustomCrosshairModule.getSpinSpeed(),
						CustomCrosshairModule.MIN_SPIN_SPEED,
						CustomCrosshairModule.MAX_SPIN_SPEED,
						CustomCrosshairModule::setSpinSpeed
				);
				y += SLIDER_HEIGHT + SETTINGS_GAP;
			}
			y += CAPSULE_GAP;
		}
	}


	private void addCombatContent() {
		int y = CONTENT_TOP;

		y = addToggleModule(
				y,
				"autoclicker",
				"autoclicker",
				Component.translatable("screen.modid.menu.combat.autoclicker"),
				AutoClickerModule.isEnabled(),
				(button, enabled) -> AutoClickerModule.setEnabled(enabled)
		);
		if (settingsOpen("autoclicker")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.autoclicker.cps"),
					AutoClickerModule.getCps(),
					AutoClickerModule.MIN_CPS,
					AutoClickerModule.MAX_CPS,
					AutoClickerModule::setCps
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP;
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.autoclicker.randomize"),
					AutoClickerModule.getRandomizeMs(),
					AutoClickerModule.MIN_RANDOMIZE,
					AutoClickerModule.MAX_RANDOMIZE,
					AutoClickerModule::setRandomizeMs
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				"velocity",
				"velocity",
				Component.translatable("screen.modid.menu.combat.velocity"),
				VelocityModule.isEnabled(),
				(button, enabled) -> VelocityModule.setEnabled(enabled)
		);
		if (settingsOpen("velocity")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.velocity.percent"),
					VelocityModule.getPercent(),
					VelocityModule.MIN_PERCENT,
					VelocityModule.MAX_PERCENT,
					VelocityModule::setPercent
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				"reach",
				"reach",
				Component.translatable("screen.modid.menu.combat.reach"),
				ReachModule.isEnabled(),
				(button, enabled) -> ReachModule.setEnabled(enabled)
		);
		if (settingsOpen("reach")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.reach.bonus"),
					ReachModule.getBonus(),
					ReachModule.MIN_BONUS,
					ReachModule.MAX_BONUS,
					ReachModule::setBonus
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				null,
				"criticals",
				Component.translatable("screen.modid.menu.combat.criticals"),
				CriticalsModule.isEnabled(),
				(button, enabled) -> CriticalsModule.setEnabled(enabled)
		);

		y = addToggleModule(
				y,
				"triggerbot",
				"triggerbot",
				Component.translatable("screen.modid.menu.combat.triggerbot"),
				TriggerBotModule.isEnabled(),
				(button, enabled) -> TriggerBotModule.setEnabled(enabled)
		);
		if (settingsOpen("triggerbot")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.triggerbot.delay"),
					TriggerBotModule.getDelay(),
					TriggerBotModule.MIN_DELAY,
					TriggerBotModule.MAX_DELAY,
					TriggerBotModule::setDelay
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				"aimassist",
				"aimassist",
				Component.translatable("screen.modid.menu.combat.aimassist"),
				AimAssistModule.isEnabled(),
				(button, enabled) -> AimAssistModule.setEnabled(enabled)
		);
		if (settingsOpen("aimassist")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.aimassist.strength"),
					AimAssistModule.getStrength(),
					AimAssistModule.MIN_STRENGTH,
					AimAssistModule.MAX_STRENGTH,
					AimAssistModule::setStrength
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP;
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.aimassist.range"),
					AimAssistModule.getRange(),
					AimAssistModule.MIN_RANGE,
					AimAssistModule.MAX_RANGE,
					AimAssistModule::setRange
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				"hitboxes",
				"hitboxes",
				Component.translatable("screen.modid.menu.combat.hitboxes"),
				HitboxesModule.isEnabled(),
				(button, enabled) -> HitboxesModule.setEnabled(enabled)
		);
		if (settingsOpen("hitboxes")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.combat.hitboxes.size"),
					HitboxesModule.getSize(),
					HitboxesModule.MIN_SIZE,
					HitboxesModule.MAX_SIZE,
					HitboxesModule::setSize
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		addToggleModule(
				y,
				null,
				"autototem",
				Component.translatable("screen.modid.menu.combat.autototem"),
				AutoTotemModule.isEnabled(),
				(button, enabled) -> AutoTotemModule.setEnabled(enabled)
		);
	}


	private void addWorldContent() {
		int y = CONTENT_TOP;
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.world.scaffold"),
				ScaffoldModule.isEnabled(),
				"scaffold",
				(button, enabled) -> ScaffoldModule.setEnabled(enabled)
		));
		int scaffoldModeIndex = switch (ScaffoldModule.getMode()) {
			case HAND_ONLY -> 0;
			case OFFHAND_ONLY -> 1;
			default -> 2;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				MODE_WIDTH + 24,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.world.scaffold.mode.hand"),
					Component.translatable("screen.modid.menu.world.scaffold.mode.offhand"),
					Component.translatable("screen.modid.menu.world.scaffold.mode.inventory")
				},
				scaffoldModeIndex,
				index -> {
					ScaffoldModule.Mode mode = switch (index) {
						case 0 -> ScaffoldModule.Mode.HAND_ONLY;
						case 1 -> ScaffoldModule.Mode.OFFHAND_ONLY;
						default -> ScaffoldModule.Mode.FROM_INVENTORY;
					};
					ScaffoldModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		y = addToggleModule(
				y,
				"fastplace",
				"fastplace",
				Component.translatable("screen.modid.menu.world.fastplace"),
				FastPlaceModule.isEnabled(),
				(button, enabled) -> FastPlaceModule.setEnabled(enabled)
		);
		if (settingsOpen("fastplace")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.world.fastplace.speed"),
					FastPlaceModule.getSpeed(),
					FastPlaceModule.MIN_SPEED,
					FastPlaceModule.MAX_SPEED,
					FastPlaceModule::setSpeed
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				null,
				"tower",
				Component.translatable("screen.modid.menu.world.tower"),
				TowerModule.isEnabled(),
				(button, enabled) -> TowerModule.setEnabled(enabled)
		);

		y = addToggleModule(
				y,
				"airplace",
				"airplace",
				Component.translatable("screen.modid.menu.world.airplace"),
				AirPlaceModule.isEnabled(),
				(button, enabled) -> AirPlaceModule.setEnabled(enabled)
		);
		if (settingsOpen("airplace")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.world.airplace.distance"),
					AirPlaceModule.getDistance(),
					AirPlaceModule.MIN_DISTANCE,
					AirPlaceModule.MAX_DISTANCE,
					AirPlaceModule::setDistance
			);
		}
	}

	private void addPlayerContent() {
		int y = CONTENT_TOP;
		y = addToggleModule(
				y,
				"inventory_move",
				"inventory_move",
				Component.translatable("screen.modid.menu.player.inventory_move"),
				InventoryMoveModule.isEnabled(),
				(button, enabled) -> InventoryMoveModule.setEnabled(enabled)
		);
		if (settingsOpen("inventory_move")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.player.inventory_move.rotate_speed"),
					InventoryMoveModule.getRotateSpeed(),
					InventoryMoveModule.MIN_ROTATE_SPEED,
					InventoryMoveModule.MAX_ROTATE_SPEED,
					InventoryMoveModule::setRotateSpeed
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}
		y = addToggleModule(
				y,
				null,
				"noslow",
				Component.translatable("screen.modid.menu.player.noslow"),
				NoSlowModule.isEnabled(),
				(button, enabled) -> NoSlowModule.setEnabled(enabled)
		);

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.player.sneak"),
				SneakModule.isEnabled(),
				"sneak",
				(button, enabled) -> SneakModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.player.sneak.mode.legit"),
					Component.translatable("screen.modid.menu.player.sneak.mode.cheat")
				},
				SneakModule.getMode() == SneakModule.Mode.CHEAT ? 1 : 0,
				index -> SneakModule.setMode(index == 1 ? SneakModule.Mode.CHEAT : SneakModule.Mode.LEGIT)
		));
	}

	private void addMiscContent() {
		addToggleModule(
				CONTENT_TOP,
				null,
				"notifications",
				Component.translatable("screen.modid.menu.misc.notifications"),
				NotificationsModule.isEnabled(),
				(button, enabled) -> NotificationsModule.setEnabled(enabled)
		);
	}

	private void addMovementContent() {
		int y = CONTENT_TOP;

		// Flight + mode dropdown + cog (speed)
		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.flight"),
				FlightModule.isEnabled(),
				"flight",
				(button, enabled) -> FlightModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CogButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				COG_SIZE,
				settingsOpen("flight"),
				button -> toggleSettings("flight")
		));
		int flightModeIndex = switch (FlightModule.getMode()) {
			case VELOCITY -> 1;
			case HOVER -> 2;
			case JETPACK -> 3;
			default -> 0;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6 + COG_SIZE + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.movement.flight.mode.vanilla"),
					Component.translatable("screen.modid.menu.movement.flight.mode.velocity"),
					Component.translatable("screen.modid.menu.movement.flight.mode.hover"),
					Component.translatable("screen.modid.menu.movement.flight.mode.jetpack")
				},
				flightModeIndex,
				index -> {
					FlightModule.Mode mode = switch (index) {
						case 1 -> FlightModule.Mode.VELOCITY;
						case 2 -> FlightModule.Mode.HOVER;
						case 3 -> FlightModule.Mode.JETPACK;
						default -> FlightModule.Mode.VANILLA;
					};
					FlightModule.setMode(mode);
					rebuildMenu();
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("flight")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.movement.flight.speed"),
					FlightModule.getSpeed(),
					FlightModule.MIN_SPEED,
					FlightModule.MAX_SPEED,
					FlightModule::setSpeed
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.elytra_control"),
				ElytraControlModule.isEnabled(),
				"elytra_control",
				(button, enabled) -> ElytraControlModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CogButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				COG_SIZE,
				settingsOpen("elytra_control"),
				button -> toggleSettings("elytra_control")
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("elytra_control")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.movement.elytra_control.speed"),
					ElytraControlModule.getSpeed(),
					ElytraControlModule.MIN_SPEED,
					ElytraControlModule.MAX_SPEED,
					ElytraControlModule::setSpeed
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.speed"),
				SpeedModule.isEnabled(),
				"speed",
				(button, enabled) -> SpeedModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new CogButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				COG_SIZE,
				settingsOpen("speed"),
				button -> toggleSettings("speed")
		));
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6 + COG_SIZE + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.movement.speed.mode.normal"),
					Component.translatable("screen.modid.menu.movement.speed.mode.strafe")
				},
				SpeedModule.getMode() == SpeedModule.Mode.STRAFE ? 1 : 0,
				index -> SpeedModule.setMode(index == 1 ? SpeedModule.Mode.STRAFE : SpeedModule.Mode.NORMAL)
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		if (settingsOpen("speed")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.movement.speed.level"),
					SpeedModule.getSpeedLevel(),
					SpeedModule.MIN_LEVEL,
					SpeedModule.MAX_LEVEL,
					SpeedModule::setSpeedLevel
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				null,
				"nofall",
				Component.translatable("screen.modid.menu.movement.nofall"),
				NoFallModule.isEnabled(),
				(button, enabled) -> NoFallModule.setEnabled(enabled)
		);

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.autosprint"),
				AutoSprintModule.isEnabled(),
				"autosprint",
				(button, enabled) -> AutoSprintModule.setEnabled(enabled)
		));
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.movement.autosprint.mode.legit"),
					Component.translatable("screen.modid.menu.movement.autosprint.mode.rage")
				},
				AutoSprintModule.getMode() == AutoSprintModule.Mode.RAGE ? 1 : 0,
				index -> AutoSprintModule.setMode(index == 1 ? AutoSprintModule.Mode.RAGE : AutoSprintModule.Mode.LEGIT)
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;

		y = addToggleModule(
				y,
				"step",
				"step",
				Component.translatable("screen.modid.menu.movement.step"),
				StepModule.isEnabled(),
				(button, enabled) -> StepModule.setEnabled(enabled)
		);
		if (settingsOpen("step")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.movement.step.height"),
					StepModule.getHeight(),
					StepModule.MIN_HEIGHT,
					StepModule.MAX_HEIGHT,
					StepModule::setHeight
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				"spider",
				"spider",
				Component.translatable("screen.modid.menu.movement.spider"),
				SpiderModule.isEnabled(),
				(button, enabled) -> SpiderModule.setEnabled(enabled)
		);
		if (settingsOpen("spider")) {
			addLabeledSlider(
					CONTENT_LEFT, y, SETTINGS_WIDTH,
					Component.translatable("screen.modid.menu.movement.spider.speed"),
					SpiderModule.getClimbSpeed(),
					SpiderModule.MIN_SPEED,
					SpiderModule.MAX_SPEED,
					SpiderModule::setClimbSpeed
			);
			y += SLIDER_HEIGHT + SETTINGS_GAP + CAPSULE_GAP;
		}

		y = addToggleModule(
				y,
				null,
				"safewalk",
				Component.translatable("screen.modid.menu.movement.safewalk"),
				SafeWalkModule.isEnabled(),
				(button, enabled) -> SafeWalkModule.setEnabled(enabled)
		);

		this.addRenderableWidget(new ToggleCapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.jesus"),
				JesusModule.isEnabled(),
				"jesus",
				(button, enabled) -> JesusModule.setEnabled(enabled)
		));
		int jesusModeIndex = switch (JesusModule.getMode()) {
			case LAVA -> 1;
			case BOTH -> 2;
			default -> 0;
		};
		this.addRenderableWidget(new ModeDropdownButton(
				CONTENT_LEFT + CAPSULE_WIDTH + 6,
				y,
				MODE_WIDTH,
				CAPSULE_HEIGHT,
				new Component[] {
					Component.translatable("screen.modid.menu.movement.jesus.mode.water"),
					Component.translatable("screen.modid.menu.movement.jesus.mode.lava"),
					Component.translatable("screen.modid.menu.movement.jesus.mode.both")
				},
				jesusModeIndex,
				index -> {
					JesusModule.Mode mode = switch (index) {
						case 1 -> JesusModule.Mode.LAVA;
						case 2 -> JesusModule.Mode.BOTH;
						default -> JesusModule.Mode.WATER;
					};
					JesusModule.setMode(mode);
					rebuildMenu();
				}
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

	/** Open dropdown steals clicks (overlays later rows); outside click closes it. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		ModeDropdownButton open = ModeDropdownButton.getOpen();
		if (open != null) {
			if (open.isMouseOver(event.x(), event.y())) {
				return open.mouseClicked(event, doubleClick);
			}
			open.close();
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Keybind capture, then Insert closes the menu while it is open. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ModuleKeybinds.handleKeyPressed(event)) {
			rebuildMenu();
			return true;
		}
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
		// Logo in title band (background stratum) so it is not covered by blur/widgets
		int logoY = Math.max(0, (TITLE_BAND - LOGO_SIZE) / 2);
		// 13-arg blit: drawSize + regionSize + texSize.
		// The 10-arg overload sets region=drawSize, which only samples the
		// top-left LOGO_SIZE×LOGO_SIZE of the 128×128 PNG (cut-off jet).
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				ROOTY_LOGO,
				LOGO_LEFT,
				logoY,
				0.0F,
				0.0F,
				LOGO_SIZE,
				LOGO_SIZE,
				LOGO_TEX_SIZE,
				LOGO_TEX_SIZE,
				LOGO_TEX_SIZE,
				LOGO_TEX_SIZE,
				0xFFFFFFFF
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();

		// Title shifted right of the logo (logo drawn in extractBackground)
		var pose = graphics.pose();
		pose.pushMatrix();
		float titleY = (TITLE_BAND - this.font.lineHeight * TITLE_SCALE) / 2.0F;
		pose.translate(TITLE_LEFT, titleY);
		pose.scale(TITLE_SCALE);
		graphics.text(this.font, this.title, 0, 0, theme.title, true);
		pose.popMatrix();

		Component panel;
		if (this.colorMenuOpen) {
			panel = Component.translatable("screen.modid.menu.colors.panel");
		} else {
			String panelKey = switch (this.selectedTab) {
				case GENERAL -> "screen.modid.menu.tab.general";
				case PLAYER -> "screen.modid.menu.tab.player";
				case VISUALS -> "screen.modid.menu.tab.visuals";
				case COMBAT -> "screen.modid.menu.tab.combat";
				case WORLD -> "screen.modid.menu.tab.world";
				case MOVEMENT -> "screen.modid.menu.tab.movement";
				case MISC -> "screen.modid.menu.tab.misc";
			};
			panel = Component.translatable("screen.modid.menu.panel", Component.translatable(panelKey));
		}
		graphics.text(this.font, panel, CONTENT_LEFT, CONTENT_TOP - 14, theme.panelHint, false);

		ModeDropdownButton open = ModeDropdownButton.getOpen();
		if (open != null) {
			open.extractOverlay(graphics, mouseX, mouseY);
		}
	}

	@Override
	public void onClose() {
		ModuleKeybinds.cancelListening();
		ModeDropdownButton.closeOpen();
		super.onClose();
	}
}
