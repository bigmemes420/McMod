package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.util.Mth;

/**
 * Visuals module: when enabled, entity nametags always render through walls
 * (including while sneaking), use the configured scale multiplier, and draw
 * a plate/background outline colored by {@link com.example.client.config.MenuTheme#nametagOutline}.
 */
public final class NametagsModule {
	public static final float MIN_SCALE = 0.5F;
	public static final float MAX_SCALE = 3.0F;
	public static final float DEFAULT_SCALE = 1.0F;

	private static boolean enabled;
	private static float scale = DEFAULT_SCALE;

	private NametagsModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getScale() {
		return scale;
	}

	public static void setScale(float value) {
		float clamped = Mth.clamp(value, MIN_SCALE, MAX_SCALE);
		if (scale == clamped) {
			return;
		}
		scale = clamped;
		ModConfig.save();
	}

	public static void loadScale(float value) {
		scale = Mth.clamp(value, MIN_SCALE, MAX_SCALE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.nametags.enabled"
						: "screen.rootymenu.menu.visuals.nametags.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}
}
