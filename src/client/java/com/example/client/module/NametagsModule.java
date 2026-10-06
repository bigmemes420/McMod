package com.example.client.module;

import net.minecraft.util.Mth;

/**
 * Visuals module: when enabled, entity nametags always render through walls
 * (including while sneaking) and use the configured scale multiplier.
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
		scale = Mth.clamp(value, MIN_SCALE, MAX_SCALE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.nametags.enabled"
						: "screen.modid.menu.visuals.nametags.disabled"
		);
	}

	public static void toggle() {
		setEnabled(!enabled);
	}
}
