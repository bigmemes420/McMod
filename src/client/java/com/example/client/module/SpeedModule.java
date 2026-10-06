package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Client Movement module: each tick, sets the local player's horizontal velocity from
 * WASD input and look yaw. Does not touch movement attributes, Timer, or global game speed.
 */
public final class SpeedModule {
	/** Horizontal blocks/tick at speed level 1. Level N => N * BASE_SPEED. */
	private static final double BASE_SPEED = 0.1D;

	public static final float MIN_LEVEL = 1.0F;
	public static final float MAX_LEVEL = 10.0F;
	public static final float DEFAULT_LEVEL = 2.0F;

	private static boolean enabled;
	private static float speedLevel = DEFAULT_LEVEL;

	private SpeedModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getSpeedLevel() {
		return speedLevel;
	}

	public static void setSpeedLevel(float level) {
		float clamped = Mth.clamp(level, MIN_LEVEL, MAX_LEVEL);
		if (speedLevel == clamped) {
			return;
		}
		speedLevel = clamped;
		ModConfig.save();
	}

	public static void loadSpeedLevel(float level) {
		speedLevel = Mth.clamp(level, MIN_LEVEL, MAX_LEVEL);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
			enabled ? "screen.modid.menu.movement.speed.enabled" : "screen.modid.menu.movement.speed.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Apply input-based horizontal velocity while enabled. Vertical motion is left alone.
	 */
	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}

		Vec2 move = player.input.getMoveVector();
		if (move.lengthSquared() < 1.0E-5F) {
			return;
		}

		float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yawRad);
		float cos = Mth.cos(yawRad);

		// move.x = strafe (left +, right -), move.y = forward — same as Entity#getInputVector
		double mx = move.x * cos - move.y * sin;
		double mz = move.y * cos + move.x * sin;

		double speed = speedLevel * BASE_SPEED;
		double len = Math.sqrt(mx * mx + mz * mz);
		if (len > 1.0E-8D) {
			mx = mx / len * speed;
			mz = mz / len * speed;
		}

		Vec3 vel = player.getDeltaMovement();
		player.setDeltaMovement(mx, vel.y, mz);
	}
}
