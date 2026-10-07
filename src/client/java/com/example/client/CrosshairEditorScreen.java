package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.module.CustomCrosshairModule;
import com.example.client.widget.FlatMenuButton;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 64×64 pixel grid editor for {@link CustomCrosshairModule}.
 * Left-click toggles cells; Save writes the pattern to ModConfig.
 */
public class CrosshairEditorScreen extends Screen {
	private static final int CELL = 6;
	private static final int GRID = CustomCrosshairModule.GRID;
	private static final int GRID_PX = GRID * CELL;
	private static final int PANEL_PAD = 16;
	private static final int BTN_W = 72;
	private static final int BTN_H = 22;

	private final Screen parent;
	private final boolean[] working = CustomCrosshairModule.copyPixels();
	private boolean painting;
	private boolean paintValue;

	private int gridLeft;
	private int gridTop;

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable("screen.modid.crosshair_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		gridLeft = (this.width - GRID_PX) / 2;
		gridTop = Math.max(36, (this.height - GRID_PX - 40) / 2);

		int btnY = Math.min(this.height - 32, gridTop + GRID_PX + 12);
		int cx = this.width / 2;
		this.addRenderableWidget(new FlatMenuButton(
				cx - BTN_W * 2 - 12,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.save"),
				button -> {
					CustomCrosshairModule.setPixelsAndSave(working);
					this.onClose();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				cx - BTN_W / 2,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.clear"),
				button -> {
					java.util.Arrays.fill(working, false);
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				cx + BTN_W / 2 + 12,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.reset"),
				button -> CustomCrosshairModule.applyDefaultPattern(working)
		));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, this.height, 0xFF0C0C12);
		int panelL = gridLeft - PANEL_PAD;
		int panelT = gridTop - PANEL_PAD - 18;
		int panelR = gridLeft + GRID_PX + PANEL_PAD;
		int panelB = gridTop + GRID_PX + PANEL_PAD + 36;
		graphics.fill(panelL, panelT, panelR, panelB, 0xFF181822);
		graphics.outline(panelL, panelT, panelR - panelL, panelB - panelT, theme.outline);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.centeredText(this.font, this.title, this.width / 2, gridTop - PANEL_PAD - 12, theme.title);

		int on = CustomCrosshairModule.getColor() | 0xFF000000;
		int off = 0xFF2A2A32;
		int gridLine = 0xFF3A3A48;
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				int px = gridLeft + x * CELL;
				int py = gridTop + y * CELL;
				graphics.fill(px, py, px + CELL, py + CELL, working[y * GRID + x] ? on : off);
			}
		}
		// Light grid every 8 cells
		for (int i = 0; i <= GRID; i += 8) {
			int x = gridLeft + i * CELL;
			int y = gridTop + i * CELL;
			graphics.fill(x, gridTop, x + 1, gridTop + GRID_PX, gridLine);
			graphics.fill(gridLeft, y, gridLeft + GRID_PX, y + 1, gridLine);
		}
		graphics.outline(gridLeft - 1, gridTop - 1, GRID_PX + 2, GRID_PX + 2, theme.outline);

		// Live preview (unrotated) next to the grid
		int previewX = gridLeft + GRID_PX + 24;
		int previewY = gridTop;
		if (previewX + GRID + 8 < this.width) {
			graphics.text(this.font, Component.translatable("screen.modid.crosshair_editor.preview"), previewX, previewY - 12, theme.panelHint, false);
			graphics.fill(previewX - 4, previewY - 4, previewX + GRID + 4, previewY + GRID + 4, 0xFF101018);
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					if (working[y * GRID + x]) {
						graphics.fill(previewX + x, previewY + y, previewX + x + 1, previewY + y + 1, on);
					}
				}
			}
		}
	}

	private boolean cellAt(double mx, double my, int[] out) {
		if (mx < gridLeft || my < gridTop || mx >= gridLeft + GRID_PX || my >= gridTop + GRID_PX) {
			return false;
		}
		out[0] = (int) ((mx - gridLeft) / CELL);
		out[1] = (int) ((my - gridTop) / CELL);
		return out[0] >= 0 && out[1] >= 0 && out[0] < GRID && out[1] < GRID;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int[] cell = new int[2];
			if (cellAt(event.x(), event.y(), cell)) {
				int i = cell[1] * GRID + cell[0];
				paintValue = !working[i];
				working[i] = paintValue;
				painting = true;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (painting && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int[] cell = new int[2];
			if (cellAt(event.x(), event.y(), cell)) {
				working[cell[1] * GRID + cell[0]] = paintValue;
				return true;
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (painting) {
			painting = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
