package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Combat module: scales knockback taken by the local player (0% = cancel,
 * 100% = vanilla). Applied client-side while hurtTime is active.
 */
public final class VelocityModule {
	public static final float MIN_PERCENT = 0.0F;
	public static final float MAX_PERCENT = 100.0F;
	public static final float DEFAULT_PERCENT = 0.0F;

	private static boolean enabled;
	private static float percent = DEFAULT_PERCENT;

	private VelocityModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getPercent() {
		return percent;
	}

	public static void setPercent(float value) {
		float clamped = Mth.clamp(value, MIN_PERCENT, MAX_PERCENT);
		if (percent == clamped) {
			return;
		}
		percent = clamped;
		ModConfig.save();
	}

	public static void loadPercent(float value) {
		percent = Mth.clamp(value, MIN_PERCENT, MAX_PERCENT);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.combat.velocity.enabled"
						: "screen.modid.menu.combat.velocity.disabled"
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
		if (player == null || player.hurtTime <= 0) {
			return;
		}
		float factor = percent / 100.0F;
		Vec3 vel = player.getDeltaMovement();
		player.setDeltaMovement(vel.x * factor, vel.y * factor, vel.z * factor);
	}
}
