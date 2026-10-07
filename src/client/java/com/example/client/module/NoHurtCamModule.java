package com.example.client.module;

import com.example.client.config.ModConfig;

/**
 * Visuals (LB NoHurtCam): skips the hurt-camera tilt when enabled.
 */
public final class NoHurtCamModule {
	private static boolean enabled;

	private NoHurtCamModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.nohurtcam.enabled"
						: "screen.rootymenu.menu.visuals.nohurtcam.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}
}
