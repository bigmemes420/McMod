package com.example.client.module;

/**
 * Visuals module: when enabled, other players render with a through-walls
 * outline colored by {@link com.example.client.config.MenuTheme#outline}.
 */
public final class PlayerOutlinesModule {
	private static boolean enabled;

	private PlayerOutlinesModule() {
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
				enabled ? "screen.modid.menu.visuals.player_outlines.enabled"
						: "screen.modid.menu.visuals.player_outlines.disabled"
		);
	}

	public static void toggle() {
		setEnabled(!enabled);
	}
}
