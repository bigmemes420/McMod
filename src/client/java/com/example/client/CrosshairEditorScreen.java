package com.example.client;

import com.example.ExampleMod;
import com.example.client.config.MenuTheme;
import com.example.client.module.CustomCrosshairModule;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.ModeDropdownButton;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.Arrays;
import java.util.List;

/**
 * Pixel grid editor + PNG picker. Grid size follows {@link CustomCrosshairModule#getGrid()}.
 * Grid/preview are baked DynamicTextures (one blit each).
 */
public class CrosshairEditorScreen extends Screen {
	private static final int PANEL_PAD = 16;
	private static final int BTN_W = 78;
	private static final int BTN_H = 22;
	private static final int BTN_GAP = 6;
	private static final int MODE_W = 100;
	private static final int DROP_W = 160;
	private static final int RES_W = 88;
	private static final int PREFERRED_CELL = 6;
	private static final int MAX_DISPLAY = 384;

	private static final int CHECK_A = 0xFF3A3A44;
	private static final int CHECK_B = 0xFF2A2A32;
	private static final int CHECK_PREVIEW_B = 0xFF101018;
	private static final int GRID_LINE = 0xFF3A3A48;

	private static final Identifier CHECKER_ID = ExampleMod.id("dynamic/crosshair_editor_checker");
	private static final Identifier WORKING_ID = ExampleMod.id("dynamic/crosshair_editor_working");

	/** Checkerboard rebuilt when resolution changes. */
	private static DynamicTexture checkerTexture;
	private static int checkerSize = -1;

	private final Screen parent;
	private boolean[] working = CustomCrosshairModule.copyPixels();
	private boolean painting;
	private boolean paintValue;
	private String localStatus = "";

	private int grid;
	private int gridPx;
	/** Pixel size of one cell when integer mapping; 0 = fractional (large grids). */
	private int cell;
	private int previewPx;
	private int gridLeft;
	private int gridTop;
	private List<String> pngNames = List.of();

	private DynamicTexture workingTexture;
	private NativeImage workingImage;
	private int workingSize = -1;
	private boolean workingDirty = true;
	private int bakedColor = Integer.MIN_VALUE;

	private Component modeLabel = Component.empty();
	private Component pathLabel = Component.empty();
	private Component statusLabel = Component.empty();
	private final Component previewTitle = Component.translatable("screen.modid.crosshair_editor.preview");

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable("screen.modid.crosshair_editor.title"));
		this.parent = parent;
	}

	private void syncWorkingBuffer() {
		boolean[] src = CustomCrosshairModule.copyPixels();
		if (working == null || working.length != src.length) {
			working = Arrays.copyOf(src, src.length);
		} else {
			System.arraycopy(src, 0, working, 0, src.length);
		}
		releaseWorkingTexture();
		markWorkingDirty();
	}

	private void layoutGrid() {
		grid = CustomCrosshairModule.getGrid();
		int maxDisp = Math.min(MAX_DISPLAY, Math.min(Math.max(64, this.width - 120), Math.max(64, this.height - 160)));
		if (grid * PREFERRED_CELL <= maxDisp) {
			cell = PREFERRED_CELL;
			gridPx = grid * cell;
		} else {
			cell = Math.max(1, maxDisp / grid);
			gridPx = cell * grid;
			if (gridPx > maxDisp) {
				gridPx = maxDisp;
				cell = 0;
			}
		}
		previewPx = Math.min(grid, Math.min(64, Math.max(16, this.width - gridPx - 80)));
		gridLeft = Math.max(PANEL_PAD, (this.width - gridPx - previewPx - 40) / 2);
		gridTop = Math.max(52, (this.height - gridPx - 80) / 2);
	}

	@Override
	protected void init() {
		ModeDropdownButton.closeOpen();
		this.clearWidgets();
		layoutGrid();
		if (working == null || working.length != grid * grid) {
			syncWorkingBuffer();
		}

		pngNames = CustomCrosshairModule.refreshPngList();
		localStatus = CustomCrosshairModule.getStatusMessage();
		refreshLabels();
		ensureCheckerTexture();
		markWorkingDirty();

		boolean pngMode = CustomCrosshairModule.getSource() == CustomCrosshairModule.Source.PNG;

		int btnY = Math.min(this.height - 68, gridTop + gridPx + 8);
		int rowMode = Math.min(this.height - 40, btnY + BTN_H + 4);

		int total = BTN_W * 3 + BTN_GAP * 2;
		int startX = this.width / 2 - total / 2;
		this.addRenderableWidget(new FlatMenuButton(
				startX, btnY, BTN_W, BTN_H,
				Component.translatable("screen.modid.crosshair_editor.save"),
				button -> {
					CustomCrosshairModule.setPixelsAndSave(working);
					localStatus = "Saved pixel pattern · Mode: Pixels";
					this.init();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX + BTN_W + BTN_GAP, btnY, BTN_W, BTN_H,
				Component.translatable("screen.modid.crosshair_editor.clear"),
				button -> {
					Arrays.fill(working, false);
					markWorkingDirty();
					localStatus = "Cleared pixels";
					refreshLabels();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				startX + (BTN_W + BTN_GAP) * 2, btnY, BTN_W, BTN_H,
				Component.translatable("screen.modid.crosshair_editor.reset"),
				button -> {
					CustomCrosshairModule.applyDefaultPattern(working);
					markWorkingDirty();
					localStatus = "Reset to default +";
					refreshLabels();
				}
		));

		CustomCrosshairModule.Resolution[] resAll = CustomCrosshairModule.Resolution.values();
		Component[] resLabels = new Component[resAll.length];
		int resIndex = 0;
		CustomCrosshairModule.Resolution curRes = CustomCrosshairModule.getResolution();
		for (int i = 0; i < resAll.length; i++) {
			resLabels[i] = Component.literal(resAll[i].label());
			if (resAll[i] == curRes) {
				resIndex = i;
			}
		}

		int modeRowWidth = RES_W + BTN_GAP + MODE_W + BTN_GAP + DROP_W + BTN_GAP + BTN_W;
		int modeX = this.width / 2 - modeRowWidth / 2;
		this.addRenderableWidget(new ModeDropdownButton(
				modeX, rowMode, RES_W, BTN_H,
				resLabels,
				resIndex,
				index -> {
					if (index < 0 || index >= resAll.length) {
						return;
					}
					CustomCrosshairModule.setResolution(resAll[index]);
					syncWorkingBuffer();
					localStatus = "Resolution " + resAll[index].label();
					this.init();
				}
		));
		int sourceIndex = pngMode ? 1 : 0;
		this.addRenderableWidget(new ModeDropdownButton(
				modeX + RES_W + BTN_GAP, rowMode, MODE_W, BTN_H,
				new Component[] {
						Component.translatable("screen.modid.menu.visuals.custom_crosshair.mode.pixels"),
						Component.translatable("screen.modid.menu.visuals.custom_crosshair.mode.png")
				},
				sourceIndex,
				index -> {
					CustomCrosshairModule.setSource(
							index == 1 ? CustomCrosshairModule.Source.PNG : CustomCrosshairModule.Source.PIXELS
					);
					syncWorkingBuffer();
					localStatus = CustomCrosshairModule.getStatusMessage();
					this.init();
				}
		));

		Component[] labels;
		int selectedIndex = 0;
		if (pngNames.isEmpty()) {
			labels = new Component[] {
					Component.translatable("screen.modid.crosshair_editor.no_pngs")
			};
		} else {
			labels = new Component[pngNames.size()];
			String selected = CustomCrosshairModule.getSelectedPng();
			for (int i = 0; i < pngNames.size(); i++) {
				labels[i] = Component.literal(pngNames.get(i));
				if (pngNames.get(i).equalsIgnoreCase(selected)) {
					selectedIndex = i;
				}
			}
		}
		this.addRenderableWidget(new ModeDropdownButton(
				modeX + RES_W + BTN_GAP + MODE_W + BTN_GAP, rowMode, DROP_W, BTN_H,
				labels, selectedIndex,
				index -> {
					if (pngNames.isEmpty() || index < 0 || index >= pngNames.size()) {
						localStatus = CustomCrosshairModule.getStatusMessage();
						refreshLabels();
						return;
					}
					boolean ok = CustomCrosshairModule.selectPng(pngNames.get(index));
					if (ok) {
						syncWorkingBuffer();
					}
					localStatus = CustomCrosshairModule.getStatusMessage();
					this.init();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				modeX + RES_W + BTN_GAP + MODE_W + BTN_GAP + DROP_W + BTN_GAP, rowMode, BTN_W, BTN_H,
				Component.translatable("screen.modid.crosshair_editor.refresh"),
				button -> {
					this.init();
					localStatus = CustomCrosshairModule.getStatusMessage();
				}
		));
	}

	private void refreshLabels() {
		boolean pngMode = CustomCrosshairModule.getSource() == CustomCrosshairModule.Source.PNG;
		boolean pngReady = CustomCrosshairModule.isPngMode();
		modeLabel = Component.literal(
				CustomCrosshairModule.getResolution().label()
						+ " · "
						+ (pngMode
						? "PNG" + (pngReady ? " — " + CustomCrosshairModule.getSelectedPng() : " (pick a file)")
						: "Pixels")
		);
		String path = CustomCrosshairModule.crosshairsDir().toString();
		if (this.font != null) {
			path = this.font.plainSubstrByWidth(path, Math.max(40, gridPx));
		}
		pathLabel = Component.literal(path);
		String status = !localStatus.isEmpty() ? localStatus : CustomCrosshairModule.getStatusMessage();
		if (this.font != null && !status.isEmpty()) {
			status = this.font.plainSubstrByWidth(status, Math.max(40, this.width - 24));
		}
		statusLabel = status.isEmpty() ? Component.empty() : Component.literal(status);
	}

	private void markWorkingDirty() {
		workingDirty = true;
	}

	private static void ensureCheckerTexture() {
		int g = CustomCrosshairModule.getGrid();
		if (checkerTexture != null && checkerSize == g) {
			return;
		}
		var client = net.minecraft.client.Minecraft.getInstance();
		if (client == null) {
			return;
		}
		NativeImage image = new NativeImage(g, g, true);
		int check = Math.max(1, g / 16);
		for (int y = 0; y < g; y++) {
			for (int x = 0; x < g; x++) {
				boolean a = ((x / check) + (y / check)) % 2 == 0;
				image.setPixel(x, y, a ? CHECK_A : CHECK_B);
			}
		}
		if (checkerTexture != null) {
			try {
				client.getTextureManager().release(CHECKER_ID);
				checkerTexture.close();
			} catch (Exception ignored) {
			}
		}
		checkerTexture = new DynamicTexture(() -> "rooty_crosshair_editor_checker", image);
		client.getTextureManager().register(CHECKER_ID, checkerTexture);
		checkerSize = g;
	}

	private void ensureWorkingTexture() {
		int color = CustomCrosshairModule.getColor();
		if (ARGB.alpha(color) == 0) {
			color = 0xFFFFFFFF;
		}
		int g = grid;
		if (workingTexture != null && workingSize == g && !workingDirty && bakedColor == color) {
			return;
		}
		var client = this.minecraft;
		if (client == null) {
			return;
		}
		if (workingTexture == null || workingImage == null || workingSize != g) {
			if (workingTexture != null) {
				try {
					client.getTextureManager().release(WORKING_ID);
					workingTexture.close();
				} catch (Exception ignored) {
				}
			}
			workingImage = new NativeImage(g, g, true);
			workingTexture = new DynamicTexture(() -> "rooty_crosshair_editor_working", workingImage);
			client.getTextureManager().register(WORKING_ID, workingTexture);
			workingSize = g;
		}
		workingImage.fillRect(0, 0, g, g, 0x00000000);
		for (int y = 0; y < g; y++) {
			for (int x = 0; x < g; x++) {
				if (working[y * g + x]) {
					workingImage.setPixel(x, y, color);
				}
			}
		}
		workingTexture.upload();
		bakedColor = color;
		workingDirty = false;
	}

	private void paintWorkingCell(int cx, int cy, boolean on) {
		working[cy * grid + cx] = on;
		int color = CustomCrosshairModule.getColor();
		if (ARGB.alpha(color) == 0) {
			color = 0xFFFFFFFF;
		}
		ensureWorkingTexture();
		if (workingImage != null && workingTexture != null) {
			workingImage.setPixel(cx, cy, on ? color : 0x00000000);
			workingTexture.upload();
			workingDirty = false;
			bakedColor = color;
		}
	}

	private void releaseWorkingTexture() {
		if (this.minecraft != null) {
			try {
				this.minecraft.getTextureManager().release(WORKING_ID);
			} catch (Exception ignored) {
			}
		}
		if (workingTexture != null) {
			try {
				workingTexture.close();
			} catch (Exception ignored) {
			}
		}
		workingTexture = null;
		workingImage = null;
		workingSize = -1;
		workingDirty = true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, this.height, 0xFF0C0C12);
		int panelL = gridLeft - PANEL_PAD;
		int panelT = gridTop - PANEL_PAD - 30;
		int panelR = gridLeft + gridPx + PANEL_PAD;
		int panelB = gridTop + gridPx + PANEL_PAD + 72;
		graphics.fill(panelL, panelT, panelR, panelB, 0xFF181822);
		graphics.outline(panelL, panelT, panelR - panelL, panelB - panelT, theme.outline);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.centeredText(this.font, this.title, this.width / 2, gridTop - PANEL_PAD - 24, theme.title);
		graphics.centeredText(this.font, modeLabel, this.width / 2, gridTop - 12, theme.panelHint);
		graphics.centeredText(this.font, pathLabel, this.width / 2, gridTop - 2, 0xFF707080);

		boolean pngReady = CustomCrosshairModule.isPngMode();
		ensureCheckerTexture();
		if (checkerSize == grid) {
			graphics.blit(
					RenderPipelines.GUI_TEXTURED, CHECKER_ID,
					gridLeft, gridTop, 0.0F, 0.0F,
					gridPx, gridPx, grid, grid, grid, grid, 0xFFFFFFFF
			);
		} else {
			graphics.fill(gridLeft, gridTop, gridLeft + gridPx, gridTop + gridPx, CHECK_B);
		}

		if (pngReady) {
			CustomCrosshairModule.drawPngPreview(graphics, gridLeft, gridTop, gridPx);
		} else {
			ensureWorkingTexture();
			if (workingTexture != null) {
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, WORKING_ID,
						gridLeft, gridTop, 0.0F, 0.0F,
						gridPx, gridPx, grid, grid, grid, grid, 0xFFFFFFFF
				);
			}
		}

		int step = Math.max(1, grid / 8);
		for (int i = 0; i <= grid; i += step) {
			int x = gridLeft + (cell > 0 ? i * cell : i * gridPx / grid);
			int y = gridTop + (cell > 0 ? i * cell : i * gridPx / grid);
			graphics.fill(x, gridTop, x + 1, gridTop + gridPx, GRID_LINE);
			graphics.fill(gridLeft, y, gridLeft + gridPx, y + 1, GRID_LINE);
		}
		graphics.outline(gridLeft - 1, gridTop - 1, gridPx + 2, gridPx + 2, theme.outline);

		int previewX = gridLeft + gridPx + 24;
		int previewY = gridTop;
		if (previewX + previewPx + 8 < this.width) {
			graphics.text(this.font, previewTitle, previewX, previewY - 12, theme.panelHint, false);
			graphics.fill(previewX, previewY, previewX + previewPx, previewY + previewPx, CHECK_PREVIEW_B);
			if (checkerSize == grid) {
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, CHECKER_ID,
						previewX, previewY, 0.0F, 0.0F,
						previewPx, previewPx, grid, grid, grid, grid, 0xFFFFFFFF
				);
			}
			if (pngReady) {
				CustomCrosshairModule.drawPngPreview(graphics, previewX, previewY, previewPx);
			} else if (workingTexture != null) {
				ensureWorkingTexture();
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, WORKING_ID,
						previewX, previewY, 0.0F, 0.0F,
						previewPx, previewPx, grid, grid, grid, grid, 0xFFFFFFFF
				);
			}
		}

		if (!statusLabel.getString().isEmpty()) {
			graphics.centeredText(this.font, statusLabel, this.width / 2, this.height - 12, theme.panelHint);
		}

		ModeDropdownButton open = ModeDropdownButton.getOpen();
		if (open != null) {
			open.extractOverlay(graphics, mouseX, mouseY);
		}
	}

	private boolean cellAt(double mx, double my, int[] out) {
		if (CustomCrosshairModule.getSource() != CustomCrosshairModule.Source.PIXELS) {
			return false;
		}
		if (mx < gridLeft || my < gridTop || mx >= gridLeft + gridPx || my >= gridTop + gridPx) {
			return false;
		}
		if (cell > 0) {
			out[0] = (int) ((mx - gridLeft) / cell);
			out[1] = (int) ((my - gridTop) / cell);
		} else {
			out[0] = (int) ((mx - gridLeft) * grid / gridPx);
			out[1] = (int) ((my - gridTop) * grid / gridPx);
		}
		return out[0] >= 0 && out[1] >= 0 && out[0] < grid && out[1] < grid;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int[] cellOut = new int[2];
			if (cellAt(event.x(), event.y(), cellOut)) {
				int i = cellOut[1] * grid + cellOut[0];
				paintValue = !working[i];
				paintWorkingCell(cellOut[0], cellOut[1], paintValue);
				painting = true;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (painting && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int[] cellOut = new int[2];
			if (cellAt(event.x(), event.y(), cellOut)) {
				paintWorkingCell(cellOut[0], cellOut[1], paintValue);
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
		ModeDropdownButton.closeOpen();
		releaseWorkingTexture();
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
