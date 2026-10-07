package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Movement: climb walls when colliding horizontally. */
public final class SpiderModule {
	public static final float MIN_SPEED = 0.1F;
	public static final float MAX_SPEED = 0.5F;
	public static final float DEFAULT_SPEED = 0.2F;

	private static boolean enabled;
	private static float climbSpeed = DEFAULT_SPEED;

	private SpiderModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getClimbSpeed() {
		return climbSpeed;
	}

	public static void setClimbSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
		if (climbSpeed == clamped) {
			return;
		}
		climbSpeed = clamped;
		ModConfig.save();
	}

	public static void loadClimbSpeed(float value) {
		climbSpeed = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.movement.spider.enabled"
						: "screen.rootymenu.menu.movement.spider.disabled"
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
		if (player == null || !player.horizontalCollision) {
			return;
		}
		Vec3 vel = player.getDeltaMovement();
		player.setDeltaMovement(vel.x, climbSpeed, vel.z);
		player.resetFallDistance();
	}
}
