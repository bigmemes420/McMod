package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.util.Mth;

/**
 * Visuals (Meteor Zoom / LB Zoom): multiplies camera FOV while enabled.
 * Scroll sensitivity omitted — use the Zoom slider. Not an anti-cheat bypass.
 */
public final class ZoomModule {
	public static final float MIN_ZOOM = 1.0F;
	public static final float MAX_ZOOM = 10.0F;
	public static final float DEFAULT_ZOOM = 4.0F;

	private static boolean enabled;
	private static float zoom = DEFAULT_ZOOM;

	private ZoomModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getZoom() {
		return zoom;
	}

	public static void setZoom(float value) {
		float clamped = Mth.clamp(value, MIN_ZOOM, MAX_ZOOM);
		if (zoom == clamped) {
			return;
		}
		zoom = clamped;
		ModConfig.save();
	}

	public static void loadZoom(float value) {
		zoom = Mth.clamp(value, MIN_ZOOM, MAX_ZOOM);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.zoom.enabled"
						: "screen.rootymenu.menu.visuals.zoom.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** Applied from {@link com.example.client.mixin.CameraMixin}. */
	public static float modifyFov(float fov) {
		if (!enabled || zoom <= 1.0F) {
			return fov;
		}
		return fov / zoom;
	}
}
