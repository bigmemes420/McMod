package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;

/**
 * Movement: AutoSprint with Legit (forward only, normal MC) / Rage (any direction).
 */
public final class AutoSprintModule {
	public enum Mode {
		LEGIT,
		RAGE
	}

	private static boolean enabled;
	private static Mode mode = Mode.LEGIT;

	private AutoSprintModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static void setMode(Mode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			mode = Mode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			mode = Mode.LEGIT;
		}
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.movement.autosprint.enabled"
						: "screen.rootymenu.menu.movement.autosprint.disabled"
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
		if (player.isShiftKeyDown() || player.isUsingItem() || player.horizontalCollision) {
			return;
		}
		Vec2 move = player.input.getMoveVector();
		boolean shouldSprint;
		if (mode == Mode.RAGE) {
			shouldSprint = move.lengthSquared() > 1.0E-4F;
		} else {
			// Legit: only while moving forward (vanilla sprint rule)
			shouldSprint = move.y > 0.0F;
		}
		if (shouldSprint && !player.isSprinting()) {
			player.setSprinting(true);
		}
	}
}
