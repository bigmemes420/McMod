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
import net.minecraft.util.ARGB;

/**
 * 64×64 pixel grid editor + PNG loader for {@link CustomCrosshairModule}.
 * Load PNG copies into {@code config/modid/crosshair.png} and switches to PNG mode
 * (full alpha via textured blit). Pixel Save switches back to grid mode.
 */
public class CrosshairEditorScreen extends Screen {
	private static final int CELL = 6;
	private static final int GRID = CustomCrosshairModule.GRID;
	private static final int GRID_PX = GRID * CELL;
	private static final int PANEL_PAD = 16;
	private static final int BTN_W = 78;
	private static final int BTN_H = 22;
	private static final int BTN_GAP = 6;

	private final Screen parent;
	private final boolean[] working = CustomCrosshairModule.copyPixels();
	private boolean painting;
	private boolean paintValue;
	private String localStatus = "";

	private int gridLeft;
	private int gridTop;

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable("screen.modid.crosshair_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.clearWidgets();
		gridLeft = (this.width - GRID_PX) / 2;
		gridTop = Math.max(40, (this.height - GRID_PX - 56) / 2);

		int btnY = Math.min(this.height - 56, gridTop + GRID_PX + 10);
		int row2 = Math.min(this.height - 30, btnY + BTN_H + 4);
		int total = BTN_W * 3 + BTN_GAP * 2;
		int startX = this.width / 2 - total / 2;

		this.addRenderableWidget(new FlatMenuButton(
				startX,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.save"),
				button -> {
					CustomCrosshairModule.setPixelsAndSave(working);
					localStatus = "Saved pixel pattern";
					this.onClose();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX + BTN_W + BTN_GAP,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.clear"),
				button -> {
					java.util.Arrays.fill(working, false);
					localStatus = "Cleared pixels";
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX + (BTN_W + BTN_GAP) * 2,
				btnY,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.reset"),
				button -> {
					CustomCrosshairModule.applyDefaultPattern(working);
					localStatus = "Reset to default +";
				}
		));

		int total2 = BTN_W * 3 + BTN_GAP * 2;
		int startX2 = this.width / 2 - total2 / 2;
		this.addRenderableWidget(new FlatMenuButton(
				startX2,
				row2,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.load_png"),
				button -> {
					localStatus = "Opening file picker…";
					CustomCrosshairModule.openLoadPngDialog(ok -> {
						if (ok) {
							System.arraycopy(CustomCrosshairModule.copyPixels(), 0, working, 0, working.length);
							localStatus = CustomCrosshairModule.getStatusMessage();
						} else {
							localStatus = CustomCrosshairModule.getStatusMessage();
						}
					});
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX2 + BTN_W + BTN_GAP,
				row2,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.reload_png"),
				button -> {
					boolean ok = CustomCrosshairModule.reloadPngFromDisk();
					if (ok) {
						System.arraycopy(CustomCrosshairModule.copyPixels(), 0, working, 0, working.length);
						com.example.client.config.ModConfig.save();
					}
					localStatus = CustomCrosshairModule.getStatusMessage();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX2 + (BTN_W + BTN_GAP) * 2,
				row2,
				BTN_W,
				BTN_H,
				Component.translatable("screen.modid.crosshair_editor.use_pixels"),
				button -> {
					CustomCrosshairModule.setSource(CustomCrosshairModule.Source.PIXELS);
					localStatus = "Using pixel grid";
				}
		));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, this.height, 0xFF0C0C12);
		int panelL = gridLeft - PANEL_PAD;
		int panelT = gridTop - PANEL_PAD - 22;
		int panelR = gridLeft + GRID_PX + PANEL_PAD;
		int panelB = gridTop + GRID_PX + PANEL_PAD + 58;
		graphics.fill(panelL, panelT, panelR, panelB, 0xFF181822);
		graphics.outline(panelL, panelT, panelR - panelL, panelB - panelT, theme.outline);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.centeredText(this.font, this.title, this.width / 2, gridTop - PANEL_PAD - 16, theme.title);

		boolean pngMode = CustomCrosshairModule.isPngMode();
		String modeLabel = pngMode
				? "Mode: PNG (" + CustomCrosshairModule.pngPath().getFileName() + ")"
				: "Mode: Pixels";
		graphics.centeredText(this.font, Component.literal(modeLabel), this.width / 2, gridTop - 6, theme.panelHint);

		int on = CustomCrosshairModule.getColor();
		if (ARGB.alpha(on) == 0) {
			on = 0xFFFFFFFF;
		}
		int off = 0xFF2A2A32;
		int gridLine = 0xFF3A3A48;

		// Checkerboard under grid so transparency is visible in PNG preview overlay
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				int px = gridLeft + x * CELL;
				int py = gridTop + y * CELL;
				boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
				graphics.fill(px, py, px + CELL, py + CELL, checker ? 0xFF3A3A44 : 0xFF2A2A32);
			}
		}

		if (pngMode) {
			// Draw PNG scaled to the grid area (alpha preserved).
			CustomCrosshairModule.drawPngPreview(graphics, gridLeft, gridTop, GRID_PX);
		} else {
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					if (!working[y * GRID + x]) {
						continue;
					}
					int px = gridLeft + x * CELL;
					int py = gridTop + y * CELL;
					graphics.fill(px, py, px + CELL, py + CELL, on | 0xFF000000);
				}
			}
		}

		for (int i = 0; i <= GRID; i += 8) {
			int x = gridLeft + i * CELL;
			int y = gridTop + i * CELL;
			graphics.fill(x, gridTop, x + 1, gridTop + GRID_PX, gridLine);
			graphics.fill(gridLeft, y, gridLeft + GRID_PX, y + 1, gridLine);
		}
		graphics.outline(gridLeft - 1, gridTop - 1, GRID_PX + 2, GRID_PX + 2, theme.outline);

		int previewX = gridLeft + GRID_PX + 24;
		int previewY = gridTop;
		if (previewX + GRID + 8 < this.width) {
			graphics.text(this.font, Component.translatable("screen.modid.crosshair_editor.preview"), previewX, previewY - 12, theme.panelHint, false);
			// Checkerboard preview bg
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
					graphics.fill(previewX + x, previewY + y, previewX + x + 1, previewY + y + 1, checker ? 0xFF3A3A44 : 0xFF101018);
				}
			}
			if (pngMode) {
				CustomCrosshairModule.drawPngPreview(graphics, previewX, previewY, GRID);
			} else {
				for (int y = 0; y < GRID; y++) {
					for (int x = 0; x < GRID; x++) {
						if (working[y * GRID + x]) {
							graphics.fill(previewX + x, previewY + y, previewX + x + 1, previewY + y + 1, on);
						}
					}
				}
			}
		}

		String status = !localStatus.isEmpty() ? localStatus : CustomCrosshairModule.getStatusMessage();
		if (!status.isEmpty()) {
			String clipped = this.font.plainSubstrByWidth(status, Math.max(40, this.width - 24));
			graphics.centeredText(this.font, Component.literal(clipped), this.width / 2, this.height - 14, theme.panelHint);
		}
	}

	private boolean cellAt(double mx, double my, int[] out) {
		if (CustomCrosshairModule.isPngMode()) {
			return false; // PNG mode: grid is display-only
		}
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
