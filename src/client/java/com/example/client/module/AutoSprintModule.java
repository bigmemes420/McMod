package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Movement: keep sprint on while moving forward. */
public final class AutoSprintModule {
	private static boolean enabled;

	private AutoSprintModule() {
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
				enabled ? "screen.modid.menu.movement.autosprint.enabled"
						: "screen.modid.menu.movement.autosprint.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}
		if (player.input.getMoveVector().y > 0.0F && !player.isSprinting() && !player.isShiftKeyDown()) {
			player.setSprinting(true);
		}
	}
}
