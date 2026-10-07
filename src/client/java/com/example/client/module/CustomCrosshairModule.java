package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * Visuals: replaces the vanilla crosshair with either a 64×64 pixel pattern or a
 * custom PNG (full ARGB alpha). Color tint, rotate, and spin apply to both.
 */
public final class CustomCrosshairModule {
	public enum Source {
		PIXELS,
		PNG
	}

	public static final int GRID = 64;
	public static final int DEFAULT_COLOR = 0xFFFFFFFF;
	public static final float MIN_SPIN_SPEED = 15.0F;
	public static final float MAX_SPIN_SPEED = 720.0F;
	public static final float DEFAULT_SPIN_SPEED = 90.0F;
	public static final int MAX_PNG_DRAW = 64;

	private static final Identifier TEXTURE_ID = ExampleMod.id("dynamic/crosshair");
	private static final String PNG_FILE_NAME = "crosshair.png";

	private static boolean enabled;
	private static Source source = Source.PIXELS;
	private static final boolean[] pixels = new boolean[GRID * GRID];
	private static int color = DEFAULT_COLOR;
	private static boolean rotate;
	private static float spinSpeed = DEFAULT_SPIN_SPEED;
	private static float angleDeg;

	private static DynamicTexture pngTexture;
	private static int pngWidth;
	private static int pngHeight;
	private static boolean pngReady;
	private static String statusMessage = "";

	static {
		applyDefaultPattern(pixels);
	}

	private CustomCrosshairModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Source getSource() {
		return source;
	}

	public static boolean isPngMode() {
		return source == Source.PNG && pngReady;
	}

	public static int getColor() {
		return color;
	}

	public static boolean isRotate() {
		return rotate;
	}

	public static float getSpinSpeed() {
		return spinSpeed;
	}

	public static String getStatusMessage() {
		return statusMessage;
	}

	public static int getPngWidth() {
		return pngWidth;
	}

	public static int getPngHeight() {
		return pngHeight;
	}

	public static Identifier getTextureId() {
		return TEXTURE_ID;
	}

	/** Canonical on-disk PNG: {@code <config>/modid/crosshair.png}. */
	public static Path pngPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(ExampleMod.MOD_ID).resolve(PNG_FILE_NAME);
	}

	public static boolean getPixel(int x, int y) {
		if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
			return false;
		}
		return pixels[y * GRID + x];
	}

	/** Copy working buffer into module state, switch to pixel mode, persist. */
	public static void setPixelsAndSave(boolean[] src) {
		if (src == null || src.length != pixels.length) {
			return;
		}
		System.arraycopy(src, 0, pixels, 0, pixels.length);
		source = Source.PIXELS;
		statusMessage = "";
		ModConfig.save();
	}

	public static boolean[] copyPixels() {
		return Arrays.copyOf(pixels, pixels.length);
	}

	public static void applyDefaultPattern(boolean[] dest) {
		Arrays.fill(dest, false);
		int c = GRID / 2;
		for (int i = c - 12; i <= c + 11; i++) {
			if (Math.abs(i - c) <= 1) {
				continue;
			}
			if (i >= 0 && i < GRID) {
				dest[c * GRID + i] = true;
				dest[i * GRID + c] = true;
			}
		}
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.custom_crosshair.enabled"
						: "screen.modid.menu.visuals.custom_crosshair.disabled"
		);
		ModConfig.save();
	}

	/** Full ARGB tint — alpha is preserved (do not force opaque). */
	public static void setColor(int argb) {
		if (color == argb) {
			return;
		}
		color = argb;
		ModConfig.save();
	}

	public static void setRotate(boolean value) {
		if (rotate == value) {
			return;
		}
		rotate = value;
		if (!rotate) {
			angleDeg = 0.0F;
		}
		ModConfig.save();
	}

	public static void setSpinSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPIN_SPEED, MAX_SPIN_SPEED);
		if (spinSpeed == clamped) {
			return;
		}
		spinSpeed = clamped;
		ModConfig.save();
	}

	public static void setSource(Source value) {
		if (value == null || source == value) {
			return;
		}
		source = value;
		if (source == Source.PNG) {
			reloadPngFromDisk();
		}
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void loadColor(int argb) {
		color = argb;
	}

	public static void loadRotate(boolean value) {
		rotate = value;
	}

	public static void loadSpinSpeed(float value) {
		spinSpeed = Mth.clamp(value, MIN_SPIN_SPEED, MAX_SPIN_SPEED);
	}

	public static void loadSource(String raw) {
		if (raw == null || raw.isBlank()) {
			source = Source.PIXELS;
			return;
		}
		try {
			source = Source.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			source = Source.PIXELS;
		}
	}

	public static void loadPixelsBase64(String raw) {
		if (raw == null || raw.isBlank()) {
			applyDefaultPattern(pixels);
			return;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(raw.trim());
			if (bytes.length < (GRID * GRID + 7) / 8) {
				applyDefaultPattern(pixels);
				return;
			}
			Arrays.fill(pixels, false);
			for (int i = 0; i < GRID * GRID; i++) {
				int b = bytes[i >>> 3] & 0xFF;
				pixels[i] = ((b >> (i & 7)) & 1) != 0;
			}
		} catch (IllegalArgumentException ex) {
			applyDefaultPattern(pixels);
		}
	}

	public static String pixelsToBase64() {
		byte[] bytes = new byte[(GRID * GRID + 7) / 8];
		for (int i = 0; i < GRID * GRID; i++) {
			if (pixels[i]) {
				bytes[i >>> 3] |= (byte) (1 << (i & 7));
			}
		}
		return Base64.getEncoder().encodeToString(bytes);
	}

	/** Called after ModConfig finishes loading module fields. */
	public static void afterConfigLoaded() {
		if (source == Source.PNG) {
			reloadPngFromDisk();
		}
	}

	public static void tick(float deltaSeconds) {
		if (!enabled || !rotate) {
			return;
		}
		angleDeg = (angleDeg + spinSpeed * deltaSeconds) % 360.0F;
		if (angleDeg < 0.0F) {
			angleDeg += 360.0F;
		}
	}

	/**
	 * Opens an OS file picker (AWT), copies the PNG into {@link #pngPath()},
	 * uploads a DynamicTexture, switches to PNG mode, and seeds the pixel grid
	 * from non-transparent texels.
	 */
	public static void openLoadPngDialog(Consumer<Boolean> onDone) {
		Thread t = new Thread(() -> {
			Path picked = null;
			try {
				FileDialog dialog = new FileDialog((Frame) null, "Select Crosshair PNG", FileDialog.LOAD);
				dialog.setFile("*.png");
				dialog.setFilenameFilter((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".png"));
				dialog.setVisible(true);
				String file = dialog.getFile();
				String dir = dialog.getDirectory();
				if (file != null && dir != null) {
					picked = Path.of(dir, file);
				}
			} catch (Throwable ignored) {
				// Fall through to config-dir file
			}
			Path finalPicked = picked;
			Minecraft.getInstance().execute(() -> {
				boolean ok;
				if (finalPicked != null && Files.isRegularFile(finalPicked)) {
					ok = importPngFile(finalPicked);
				} else {
					ok = reloadPngFromDisk();
					if (!ok) {
						statusMessage = "Place a PNG at " + pngPath() + " or pick a file";
					}
				}
				if (onDone != null) {
					onDone.accept(ok);
				}
			});
		}, "rooty-crosshair-png-picker");
		t.setDaemon(true);
		t.start();
	}

	/** Copy {@code file} → config PNG and activate PNG mode. */
	public static boolean importPngFile(Path file) {
		try {
			Path dest = pngPath();
			Files.createDirectories(dest.getParent());
			Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
			boolean ok = loadPngTexture(dest);
			if (ok) {
				source = Source.PNG;
				statusMessage = "Loaded " + file.getFileName();
				ModConfig.save();
			} else {
				statusMessage = "Failed to decode PNG";
			}
			return ok;
		} catch (IOException e) {
			ExampleMod.LOGGER.warn("Crosshair PNG import failed: {}", e.toString());
			statusMessage = "Import failed: " + e.getMessage();
			return false;
		}
	}

	public static boolean reloadPngFromDisk() {
		Path path = pngPath();
		if (!Files.isRegularFile(path)) {
			pngReady = false;
			statusMessage = "No PNG at " + path;
			return false;
		}
		boolean ok = loadPngTexture(path);
		if (ok) {
			source = Source.PNG;
			statusMessage = "Using " + PNG_FILE_NAME;
		}
		return ok;
	}

	private static boolean loadPngTexture(Path path) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		try (InputStream in = Files.newInputStream(path)) {
			NativeImage image = NativeImage.read(in);
			seedPixelsFromImage(image);
			releasePngTexture();
			pngTexture = new DynamicTexture(() -> "rooty_crosshair", image);
			client.getTextureManager().register(TEXTURE_ID, pngTexture);
			pngWidth = image.getWidth();
			pngHeight = image.getHeight();
			pngReady = pngWidth > 0 && pngHeight > 0;
			return pngReady;
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to load crosshair PNG {}: {}", path, e.toString());
			pngReady = false;
			return false;
		}
	}

	private static void seedPixelsFromImage(NativeImage image) {
		Arrays.fill(pixels, false);
		int w = image.getWidth();
		int h = image.getHeight();
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				int sx = x * w / GRID;
				int sy = y * h / GRID;
				sx = Mth.clamp(sx, 0, w - 1);
				sy = Mth.clamp(sy, 0, h - 1);
				int argb = image.getPixel(sx, sy);
				if (ARGB.alpha(argb) > 32) {
					pixels[y * GRID + x] = true;
				}
			}
		}
	}

	private static void releasePngTexture() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			try {
				client.getTextureManager().release(TEXTURE_ID);
			} catch (Exception ignored) {
			}
		}
		if (pngTexture != null) {
			try {
				pngTexture.close();
			} catch (Exception ignored) {
			}
			pngTexture = null;
		}
		pngReady = false;
	}

	/** Fabric HUD element — draws the custom crosshair centered on screen. */
	public static void extractHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) {
			return;
		}
		if (client.gui.screen() != null) {
			return;
		}
		float dt = delta.getRealtimeDeltaTicks() / 20.0F;
		tick(dt);

		int sw = client.getWindow().getGuiScaledWidth();
		int sh = client.getWindow().getGuiScaledHeight();
		float cx = sw / 2.0F;
		float cy = sh / 2.0F;

		if (source == Source.PNG && pngReady) {
			drawPng(graphics, cx, cy);
		} else {
			drawPixels(graphics, cx, cy);
		}
	}

	private static void drawPng(GuiGraphicsExtractor graphics, float cx, float cy) {
		int drawW = pngWidth;
		int drawH = pngHeight;
		int maxSide = Math.max(drawW, drawH);
		if (maxSide > MAX_PNG_DRAW) {
			float scale = MAX_PNG_DRAW / (float) maxSide;
			drawW = Math.max(1, Math.round(drawW * scale));
			drawH = Math.max(1, Math.round(drawH * scale));
		}
		float angle = rotate ? angleDeg * Mth.DEG_TO_RAD : 0.0F;
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		if (angle != 0.0F) {
			pose.rotate(angle);
		}
		pose.translate(-drawW / 2.0F, -drawH / 2.0F);
		// GUI_TEXTURED respects texture alpha; tint keeps caller's ARGB (incl. alpha).
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_ID,
				0,
				0,
				0.0F,
				0.0F,
				drawW,
				drawH,
				pngWidth,
				pngHeight,
				pngWidth,
				pngHeight,
				color
		);
		pose.popMatrix();
	}

	private static void drawPixels(GuiGraphicsExtractor graphics, float cx, float cy) {
		float angle = rotate ? angleDeg * Mth.DEG_TO_RAD : 0.0F;
		float cos = Mth.cos(angle);
		float sin = Mth.sin(angle);
		float mid = (GRID - 1) / 2.0F;
		// Preserve alpha from the color picker (do not OR with 0xFF000000).
		int argb = color;
		if (ARGB.alpha(argb) == 0) {
			return;
		}
		for (int py = 0; py < GRID; py++) {
			for (int px = 0; px < GRID; px++) {
				if (!pixels[py * GRID + px]) {
					continue;
				}
				float dx = px - mid;
				float dy = py - mid;
				float rx = dx * cos - dy * sin;
				float ry = dx * sin + dy * cos;
				int sx = Math.round(cx + rx);
				int sy = Math.round(cy + ry);
				graphics.fill(sx, sy, sx + 1, sy + 1, argb);
			}
		}
	}

	/** Preview helper for the editor (unrotated, top-left at x,y). Fits into maxSide. */
	public static void drawPngPreview(GuiGraphicsExtractor graphics, int x, int y, int maxSide) {
		if (!pngReady) {
			return;
		}
		int side = Math.max(pngWidth, pngHeight);
		float scale = maxSide / (float) Math.max(1, side);
		int drawW = Math.max(1, Math.round(pngWidth * scale));
		int drawH = Math.max(1, Math.round(pngHeight * scale));
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_ID,
				x,
				y,
				0.0F,
				0.0F,
				drawW,
				drawH,
				pngWidth,
				pngHeight,
				pngWidth,
				pngHeight,
				color
		);
	}

	public static void registerHud() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				ExampleMod.id("custom_crosshair"),
				CustomCrosshairModule::extractHud
		);
	}
}
