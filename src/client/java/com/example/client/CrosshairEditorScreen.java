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

import java.util.List;

/**
 * 64×64 pixel grid editor + PNG picker. Grid/preview are baked DynamicTextures
 * (one blit each) — not thousands of per-cell fills every frame.
 */
public class CrosshairEditorScreen extends Screen {
	private static final int CELL = 6;
	private static final int GRID = CustomCrosshairModule.GRID;
	private static final int GRID_PX = GRID * CELL;
	private static final int PANEL_PAD = 16;
	private static final int BTN_W = 78;
	private static final int BTN_H = 22;
	private static final int BTN_GAP = 6;
	private static final int MODE_W = 100;
	private static final int DROP_W = 160;

	private static final int CHECK_A = 0xFF3A3A44;
	private static final int CHECK_B = 0xFF2A2A32;
	private static final int CHECK_PREVIEW_B = 0xFF101018;
	private static final int GRID_LINE = 0xFF3A3A48;

	private static final Identifier CHECKER_ID = ExampleMod.id("dynamic/crosshair_editor_checker");
	private static final Identifier WORKING_ID = ExampleMod.id("dynamic/crosshair_editor_working");

	/** Shared 64×64 checkerboard — built once for the process. */
	private static DynamicTexture checkerTexture;
	private static boolean checkerReady;

	private final Screen parent;
	private final boolean[] working = CustomCrosshairModule.copyPixels();
	private boolean painting;
	private boolean paintValue;
	private String localStatus = "";

	private int gridLeft;
	private int gridTop;
	private List<String> pngNames = List.of();

	private DynamicTexture workingTexture;
	private NativeImage workingImage;
	private boolean workingDirty = true;
	private int bakedColor = Integer.MIN_VALUE;

	/** Cached labels to avoid allocating Components every frame. */
	private Component modeLabel = Component.empty();
	private Component pathLabel = Component.empty();
	private Component statusLabel = Component.empty();
	private final Component previewTitle = Component.translatable("screen.modid.crosshair_editor.preview");

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable("screen.modid.crosshair_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModeDropdownButton.closeOpen();
		this.clearWidgets();
		gridLeft = (this.width - GRID_PX) / 2;
		gridTop = Math.max(52, (this.height - GRID_PX - 72) / 2);

		pngNames = CustomCrosshairModule.refreshPngList();
		localStatus = CustomCrosshairModule.getStatusMessage();
		refreshLabels();
		ensureCheckerTexture();
		markWorkingDirty();

		boolean pngMode = CustomCrosshairModule.getSource() == CustomCrosshairModule.Source.PNG;

		int btnY = Math.min(this.height - 68, gridTop + GRID_PX + 8);
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
					java.util.Arrays.fill(working, false);
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

		int modeRowWidth = MODE_W + BTN_GAP + DROP_W + BTN_GAP + BTN_W;
		int modeX = this.width / 2 - modeRowWidth / 2;
		int sourceIndex = pngMode ? 1 : 0;
		this.addRenderableWidget(new ModeDropdownButton(
				modeX, rowMode, MODE_W, BTN_H,
				new Component[] {
						Component.translatable("screen.modid.menu.visuals.custom_crosshair.mode.pixels"),
						Component.translatable("screen.modid.menu.visuals.custom_crosshair.mode.png")
				},
				sourceIndex,
				index -> {
					CustomCrosshairModule.setSource(
							index == 1 ? CustomCrosshairModule.Source.PNG : CustomCrosshairModule.Source.PIXELS
					);
					if (index == 1) {
						System.arraycopy(CustomCrosshairModule.copyPixels(), 0, working, 0, working.length);
						markWorkingDirty();
					}
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
				modeX + MODE_W + BTN_GAP, rowMode, DROP_W, BTN_H,
				labels, selectedIndex,
				index -> {
					if (pngNames.isEmpty() || index < 0 || index >= pngNames.size()) {
						localStatus = CustomCrosshairModule.getStatusMessage();
						refreshLabels();
						return;
					}
					boolean ok = CustomCrosshairModule.selectPng(pngNames.get(index));
					if (ok) {
						System.arraycopy(CustomCrosshairModule.copyPixels(), 0, working, 0, working.length);
						markWorkingDirty();
					}
					localStatus = CustomCrosshairModule.getStatusMessage();
					this.init();
				}
		));
		this.addRenderableWidget(new FlatMenuButton(
				modeX + MODE_W + BTN_GAP + DROP_W + BTN_GAP, rowMode, BTN_W, BTN_H,
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
		modeLabel = Component.literal(pngMode
				? "Mode: PNG" + (pngReady ? " — " + CustomCrosshairModule.getSelectedPng() : " (pick a file)")
				: "Mode: Pixels");
		String path = CustomCrosshairModule.crosshairsDir().toString();
		if (this.font != null) {
			path = this.font.plainSubstrByWidth(path, Math.max(40, GRID_PX));
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
		if (checkerReady && checkerTexture != null) {
			return;
		}
		var client = net.minecraft.client.Minecraft.getInstance();
		if (client == null) {
			return;
		}
		NativeImage image = new NativeImage(GRID, GRID, true);
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				boolean a = ((x / 4) + (y / 4)) % 2 == 0;
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
		checkerReady = true;
	}

	private void ensureWorkingTexture() {
		int color = CustomCrosshairModule.getColor();
		if (ARGB.alpha(color) == 0) {
			color = 0xFFFFFFFF;
		}
		if (workingTexture != null && !workingDirty && bakedColor == color) {
			return;
		}
		var client = this.minecraft;
		if (client == null) {
			return;
		}
		if (workingTexture == null || workingImage == null) {
			if (workingTexture != null) {
				try {
					client.getTextureManager().release(WORKING_ID);
					workingTexture.close();
				} catch (Exception ignored) {
				}
			}
			workingImage = new NativeImage(GRID, GRID, true);
			workingTexture = new DynamicTexture(() -> "rooty_crosshair_editor_working", workingImage);
			client.getTextureManager().register(WORKING_ID, workingTexture);
		}
		workingImage.fillRect(0, 0, GRID, GRID, 0x00000000);
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (working[y * GRID + x]) {
					workingImage.setPixel(x, y, color);
				}
			}
		}
		workingTexture.upload();
		bakedColor = color;
		workingDirty = false;
	}

	/** Update a single cell in the baked texture (paint path — no full rebuild). */
	private void paintWorkingCell(int cx, int cy, boolean on) {
		working[cy * GRID + cx] = on;
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
		workingDirty = true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, this.height, 0xFF0C0C12);
		int panelL = gridLeft - PANEL_PAD;
		int panelT = gridTop - PANEL_PAD - 30;
		int panelR = gridLeft + GRID_PX + PANEL_PAD;
		int panelB = gridTop + GRID_PX + PANEL_PAD + 72;
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
		// One blit for checkerboard (was 4096 fills).
		if (checkerReady) {
			graphics.blit(
					RenderPipelines.GUI_TEXTURED, CHECKER_ID,
					gridLeft, gridTop, 0.0F, 0.0F,
					GRID_PX, GRID_PX, GRID, GRID, GRID, GRID, 0xFFFFFFFF
			);
		} else {
			graphics.fill(gridLeft, gridTop, gridLeft + GRID_PX, gridTop + GRID_PX, CHECK_B);
		}

		if (pngReady) {
			CustomCrosshairModule.drawPngPreview(graphics, gridLeft, gridTop, GRID_PX);
		} else {
			ensureWorkingTexture();
			if (workingTexture != null) {
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, WORKING_ID,
						gridLeft, gridTop, 0.0F, 0.0F,
						GRID_PX, GRID_PX, GRID, GRID, GRID, GRID, 0xFFFFFFFF
				);
			}
		}

		// Sparse grid lines every 8 cells (17 lines each axis — cheap).
		for (int i = 0; i <= GRID; i += 8) {
			int x = gridLeft + i * CELL;
			int y = gridTop + i * CELL;
			graphics.fill(x, gridTop, x + 1, gridTop + GRID_PX, GRID_LINE);
			graphics.fill(gridLeft, y, gridLeft + GRID_PX, y + 1, GRID_LINE);
		}
		graphics.outline(gridLeft - 1, gridTop - 1, GRID_PX + 2, GRID_PX + 2, theme.outline);

		int previewX = gridLeft + GRID_PX + 24;
		int previewY = gridTop;
		if (previewX + GRID + 8 < this.width) {
			graphics.text(this.font, previewTitle, previewX, previewY - 12, theme.panelHint, false);
			// Solid preview bg + one blit (no 4096 checker fills).
			graphics.fill(previewX, previewY, previewX + GRID, previewY + GRID, CHECK_PREVIEW_B);
			if (checkerReady) {
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, CHECKER_ID,
						previewX, previewY, 0.0F, 0.0F,
						GRID, GRID, GRID, GRID, GRID, GRID, 0xFFFFFFFF
				);
			}
			if (pngReady) {
				CustomCrosshairModule.drawPngPreview(graphics, previewX, previewY, GRID);
			} else if (workingTexture != null) {
				ensureWorkingTexture();
				graphics.blit(
						RenderPipelines.GUI_TEXTURED, WORKING_ID,
						previewX, previewY, 0.0F, 0.0F,
						GRID, GRID, GRID, GRID, GRID, GRID, 0xFFFFFFFF
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
				paintWorkingCell(cell[0], cell[1], paintValue);
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
				paintWorkingCell(cell[0], cell[1], paintValue);
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
