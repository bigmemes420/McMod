package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;

/**
 * Movement (Meteor AutoWalk simple): holds forward movement while enabled.
 */
public final class AutoWalkModule {
	private static boolean enabled;

	private AutoWalkModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!enabled) {
			Minecraft client = Minecraft.getInstance();
			if (client != null && client.options != null) {
				client.options.keyUp.setDown(false);
			}
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.movement.autowalk.enabled"
						: "screen.rootymenu.menu.movement.autowalk.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled || client.player == null || client.options == null) {
			return;
		}
		if (client.gui.screen() != null) {
			return;
		}
		client.options.keyUp.setDown(true);
	}
}
