package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

/**
 * Movement: walk on water / lava surface (client-side). Modes via the same
 * module toggle; fluid type filtered by mode dropdown in the menu.
 */
public final class JesusModule {
	public enum Mode {
		WATER,
		LAVA,
		BOTH;

		public static Mode fromString(String raw) {
			if (raw == null || raw.isBlank()) {
				return WATER;
			}
			try {
				return Mode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return WATER;
			}
		}
	}

	private static boolean enabled;
	private static Mode mode = Mode.WATER;

	private JesusModule() {
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
		mode = Mode.fromString(raw);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.movement.jesus.enabled"
						: "screen.modid.menu.movement.jesus.disabled"
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
		if (player == null || client.level == null || player.input == null) {
			return;
		}
		if (player.input.keyPresses.shift()) {
			return; // allow sinking while sneak
		}
		boolean inWater = player.isEyeInFluid(FluidTags.WATER) || player.isInWater();
		boolean inLava = player.isEyeInFluid(FluidTags.LAVA) || player.isInLava();
		boolean apply = switch (mode) {
			case WATER -> inWater;
			case LAVA -> inLava;
			case BOTH -> inWater || inLava;
		};
		if (!apply) {
			return;
		}
		Vec3 vel = player.getDeltaMovement();
		if (vel.y < 0.0D) {
			player.setDeltaMovement(vel.x, 0.0D, vel.z);
		}
		player.setOnGround(true);
		player.resetFallDistance();
		if (player.input.keyPresses.jump()) {
			player.setDeltaMovement(player.getDeltaMovement().x, 0.4D, player.getDeltaMovement().z);
		}
	}
}
