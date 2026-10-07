package com.example.client.module;

import com.example.client.config.ModConfig;

/**
 * Visuals module: when enabled, forces full scene brightness via the lightmap
 * (night-vision-intensity path) so caves and night are fully lit client-side.
 */
public final class FullbrightModule {
	private static boolean enabled;

	private FullbrightModule() {
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
				enabled ? "screen.rootymenu.menu.visuals.fullbright.enabled"
						: "screen.rootymenu.menu.visuals.fullbright.disabled"
		);
		ModConfig.save();
	}

	/** Apply persisted state without notifying or re-saving. */
	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}
}
