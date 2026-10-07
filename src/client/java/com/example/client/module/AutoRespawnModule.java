package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/**
 * Player (Meteor AutoRespawn): automatically respawns when the death screen opens.
 */
public final class AutoRespawnModule {
	private static boolean enabled;

	private AutoRespawnModule() {
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
				enabled ? "screen.rootymenu.menu.player.autorespawn.enabled"
						: "screen.rootymenu.menu.player.autorespawn.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled || client.player == null) {
			return;
		}
		if (client.gui.screen() instanceof DeathScreen) {
			client.player.respawn();
			client.gui.setScreen(null);
		}
	}
}
