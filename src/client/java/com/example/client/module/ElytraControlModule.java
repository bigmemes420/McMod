package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

/**
 * Client Movement module: while gliding with an elytra ({@code isFallFlying}),
 * overrides velocity from look direction and WASD / jump / sneak, scaled by the
 * speed slider. Does nothing when not fall-flying.
 */
public final class ElytraControlModule {
	public static final float MIN_SPEED = 0.5F;
	public static final float MAX_SPEED = 5.0F;
	public static final float DEFAULT_SPEED = 1.5F;

	/** Blocks/tick at speed 1.0 while fall-flying. */
	private static final double BASE_SPEED = 0.55D;

	private static boolean enabled;
	private static float speed = DEFAULT_SPEED;

	private ElytraControlModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getSpeed() {
		return speed;
	}

	public static void setSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
		if (speed == clamped) {
			return;
		}
		speed = clamped;
		ModConfig.save();
	}

	public static void loadSpeed(float value) {
		speed = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.movement.elytra_control.enabled"
						: "screen.modid.menu.movement.elytra_control.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || player.input == null || !player.isFallFlying()) {
			return;
		}

		double fly = speed * BASE_SPEED;
		Vec2 move = player.input.getMoveVector();
		float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
		float pitchRad = player.getXRot() * Mth.DEG_TO_RAD;
		float cosYaw = Mth.cos(yawRad);
		float sinYaw = Mth.sin(yawRad);
		float cosPitch = Mth.cos(pitchRad);
		float sinPitch = Mth.sin(pitchRad);

		// Look-forward basis (same as creative fly / Firework boost feel).
		double fx = -sinYaw * cosPitch;
		double fy = -sinPitch;
		double fz = cosYaw * cosPitch;
		// Horizontal right strafe.
		double rx = cosYaw;
		double rz = sinYaw;

		double mx = 0.0D;
		double my = 0.0D;
		double mz = 0.0D;

		if (move.lengthSquared() > 1.0E-5F) {
			// move.y = forward, move.x = strafe
			mx += fx * move.y * fly + rx * move.x * fly;
			my += fy * move.y * fly;
			mz += fz * move.y * fly + rz * move.x * fly;
		}

		if (player.input.keyPresses.jump()) {
			my += fly;
		}
		if (player.input.keyPresses.shift()) {
			my -= fly;
		}

		// If no input, gently keep look-direction cruise so gliding does not stall.
		if (Math.abs(mx) + Math.abs(my) + Math.abs(mz) < 1.0E-8D) {
			mx = fx * fly * 0.35D;
			my = fy * fly * 0.35D;
			mz = fz * fly * 0.35D;
		} else {
			double len = Math.sqrt(mx * mx + my * my + mz * mz);
			if (len > fly && len > 1.0E-8D) {
				mx = mx / len * fly;
				my = my / len * fly;
				mz = mz / len * fly;
			}
		}

		player.setDeltaMovement(mx, my, mz);
		player.resetFallDistance();
	}
}
