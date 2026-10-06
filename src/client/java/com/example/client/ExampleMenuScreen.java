package com.example.client;

import com.example.client.module.FlightModule;
import com.example.client.widget.CapsuleButton;
import com.example.client.widget.FlatMenuButton;
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
	private static final int CLOSE_HEIGHT = 20;
	private static final int CAPSULE_WIDTH = 200;
	private static final int CAPSULE_HEIGHT = 24;
	private static final int CONTENT_TOP = 56;
	private static final int CONTENT_LEFT = 24;
	private static final int CAPSULE_GAP = 8;

	private Tab selectedTab = Tab.GENERAL;

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

		// Close in the top-right corner
		this.addRenderableWidget(new FlatMenuButton(
				this.width - CLOSE_WIDTH - 8,
				(TOP_BAR_HEIGHT - CLOSE_HEIGHT) / 2,
				CLOSE_WIDTH,
				CLOSE_HEIGHT,
				Component.translatable("screen.modid.menu.close"),
				button -> this.onClose()
		));

		addContentButtons();
	}

	private void addTab(int x, int y, Tab tab, String translationKey) {
		boolean selected = this.selectedTab == tab;
		this.addRenderableWidget(new FlatMenuButton(
				x,
				y,
				TAB_WIDTH,
				TAB_HEIGHT,
				Component.translatable(translationKey),
				selected,
				button -> {
					if (this.selectedTab != tab) {
						this.selectedTab = tab;
						rebuildMenu();
					}
				}
		));
	}

	private void addContentButtons() {
		if (this.selectedTab == Tab.MOVEMENT) {
			addMovementContent();
			return;
		}

		String[] keys = switch (this.selectedTab) {
			case GENERAL -> new String[] {
					"screen.modid.menu.general.option1",
					"screen.modid.menu.general.option2",
					"screen.modid.menu.general.option3"
			};
			case VISUALS -> new String[] {
					"screen.modid.menu.visuals.option1",
					"screen.modid.menu.visuals.option2",
					"screen.modid.menu.visuals.option3"
			};
			case MISC -> new String[] {
					"screen.modid.menu.misc.option1",
					"screen.modid.menu.misc.option2",
					"screen.modid.menu.misc.option3"
			};
			case MOVEMENT -> new String[] {};
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
						// Placeholder actions for submenu options.
					}
			));
			y += CAPSULE_HEIGHT + CAPSULE_GAP;
		}
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

		// Minimal placeholders
		this.addRenderableWidget(new CapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.speed"),
				button -> {
				}
		));
		y += CAPSULE_HEIGHT + CAPSULE_GAP;
		this.addRenderableWidget(new CapsuleButton(
				CONTENT_LEFT,
				y,
				CAPSULE_WIDTH,
				CAPSULE_HEIGHT,
				Component.translatable("screen.modid.menu.movement.nofall"),
				button -> {
				}
		));
	}

	/**
	 * Draw the top bar in the background stratum so tab/close widgets (next stratum)
	 * render above the bar fill and any menu dimming/blur.
	 */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, 0xCC101018);
		graphics.horizontalLine(0, this.width - 1, TOP_BAR_HEIGHT, 0xFF404050);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		// Title below the top bar, left-aligned near content
		graphics.text(this.font, this.title, CONTENT_LEFT, TOP_BAR_HEIGHT + 8, 0xFFFFFFFF, true);

		String panelKey = switch (this.selectedTab) {
			case GENERAL -> "screen.modid.menu.tab.general";
			case VISUALS -> "screen.modid.menu.tab.visuals";
			case MOVEMENT -> "screen.modid.menu.tab.movement";
			case MISC -> "screen.modid.menu.tab.misc";
		};
		graphics.text(
				this.font,
				Component.translatable("screen.modid.menu.panel", Component.translatable(panelKey)),
				CONTENT_LEFT,
				CONTENT_TOP - 14,
				0xFFAAAAAA,
				false
		);
	}
}
