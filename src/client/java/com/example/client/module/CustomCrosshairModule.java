package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Base64;

/**
 * Visuals: replaces the vanilla crosshair with a custom 64×64 pixel pattern,
 * optional color tint, and optional spin.
 */
public final class CustomCrosshairModule {
	public static final int GRID = 64;
	public static final int DEFAULT_COLOR = 0xFFFFFFFF;
	public static final float MIN_SPIN_SPEED = 15.0F;
	public static final float MAX_SPIN_SPEED = 720.0F;
	public static final float DEFAULT_SPIN_SPEED = 90.0F;

	private static boolean enabled;
	private static final boolean[] pixels = new boolean[GRID * GRID];
	private static int color = DEFAULT_COLOR;
	private static boolean rotate;
	private static float spinSpeed = DEFAULT_SPIN_SPEED;
	private static float angleDeg;

	static {
		applyDefaultPattern(pixels);
	}

	private CustomCrosshairModule() {
	}

	public static boolean isEnabled() {
		return enabled;
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

	public static boolean getPixel(int x, int y) {
		if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
			return false;
		}
		return pixels[y * GRID + x];
	}

	public static void setPixel(int x, int y, boolean on) {
		if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
			return;
		}
		pixels[y * GRID + x] = on;
	}

	public static void togglePixel(int x, int y) {
		if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
			return;
		}
		int i = y * GRID + x;
		pixels[i] = !pixels[i];
	}

	/** Copy working buffer into module state and persist. */
	public static void setPixelsAndSave(boolean[] src) {
		if (src == null || src.length != pixels.length) {
			return;
		}
		System.arraycopy(src, 0, pixels, 0, pixels.length);
		ModConfig.save();
	}

	public static boolean[] copyPixels() {
		return Arrays.copyOf(pixels, pixels.length);
	}

	public static void clearPixels() {
		Arrays.fill(pixels, false);
	}

	public static void resetDefaultPattern() {
		applyDefaultPattern(pixels);
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

	public static void setColor(int argb) {
		int opaque = ARGB.opaque(argb);
		if (color == opaque) {
			return;
		}
		color = opaque;
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

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void loadColor(int argb) {
		color = ARGB.opaque(argb);
	}

	public static void loadRotate(boolean value) {
		rotate = value;
	}

	public static void loadSpinSpeed(float value) {
		spinSpeed = Mth.clamp(value, MIN_SPIN_SPEED, MAX_SPIN_SPEED);
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

	public static void tick(float deltaSeconds) {
		if (!enabled || !rotate) {
			return;
		}
		angleDeg = (angleDeg + spinSpeed * deltaSeconds) % 360.0F;
		if (angleDeg < 0.0F) {
			angleDeg += 360.0F;
		}
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
		float angle = rotate ? angleDeg * Mth.DEG_TO_RAD : 0.0F;
		float cos = Mth.cos(angle);
		float sin = Mth.sin(angle);
		float mid = (GRID - 1) / 2.0F;
		int argb = color | 0xFF000000;

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

	public static void registerHud() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				ExampleMod.id("custom_crosshair"),
				CustomCrosshairModule::extractHud
		);
	}

	/** Classic + with a small gap in the middle. */
	public static void applyDefaultPattern(boolean[] dest) {
		Arrays.fill(dest, false);
		int c = GRID / 2;
		for (int i = c - 12; i <= c + 11; i++) {
			if (Math.abs(i - c) <= 1) {
				continue; // gap at center
			}
			if (i >= 0 && i < GRID) {
				dest[c * GRID + i] = true;
				dest[i * GRID + c] = true;
			}
		}
	}
}
