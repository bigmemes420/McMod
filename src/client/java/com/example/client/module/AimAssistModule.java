package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Combat module: softly rotates the camera toward the nearest living entity
 * inside the configured FOV / range. Strength slider = lerp factor percent.
 */
public final class AimAssistModule {
	public static final float MIN_STRENGTH = 1.0F;
	public static final float MAX_STRENGTH = 100.0F;
	public static final float DEFAULT_STRENGTH = 25.0F;

	public static final float MIN_RANGE = 2.0F;
	public static final float MAX_RANGE = 8.0F;
	public static final float DEFAULT_RANGE = 4.5F;

	private static boolean enabled;
	private static float strength = DEFAULT_STRENGTH;
	private static float range = DEFAULT_RANGE;

	private AimAssistModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getStrength() {
		return strength;
	}

	public static float getRange() {
		return range;
	}

	public static void setStrength(float value) {
		float clamped = Mth.clamp(value, MIN_STRENGTH, MAX_STRENGTH);
		if (strength == clamped) {
			return;
		}
		strength = clamped;
		ModConfig.save();
	}

	public static void loadStrength(float value) {
		strength = Mth.clamp(value, MIN_STRENGTH, MAX_STRENGTH);
	}

	public static void setRange(float value) {
		float clamped = Mth.clamp(value, MIN_RANGE, MAX_RANGE);
		if (range == clamped) {
			return;
		}
		range = clamped;
		ModConfig.save();
	}

	public static void loadRange(float value) {
		range = Mth.clamp(value, MIN_RANGE, MAX_RANGE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.aimassist.enabled"
						: "screen.rootymenu.menu.combat.aimassist.disabled"
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
		if (player == null || client.level == null || client.gui.screen() != null) {
			return;
		}

		LivingEntity best = null;
		double bestAngle = Double.MAX_VALUE;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		double rangeSq = range * range;

		AABB box = player.getBoundingBox().inflate(range);
		for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, box, e ->
				e.isAlive() && e != player && !e.isInvisibleTo(player))) {
			double distSq = player.distanceToSqr(entity);
			if (distSq > rangeSq) {
				continue;
			}
			Vec3 to = entity.getEyePosition().subtract(eye).normalize();
			double angle = Math.acos(Mth.clamp(look.dot(to), -1.0, 1.0));
			if (angle > Math.toRadians(60.0)) {
				continue;
			}
			if (angle < bestAngle) {
				bestAngle = angle;
				best = entity;
			}
		}

		if (best == null) {
			return;
		}

		Vec3 target = best.getEyePosition().subtract(eye);
		double dist = Math.sqrt(target.x * target.x + target.z * target.z);
		float wantYaw = (float) (Math.toDegrees(Math.atan2(-target.x, target.z)));
		float wantPitch = (float) (Math.toDegrees(-Math.atan2(target.y, dist)));
		float t = strength / 100.0F;
		float newYaw = lerpAngle(player.getYRot(), wantYaw, t);
		float newPitch = Mth.lerp(t, player.getXRot(), wantPitch);
		player.setYRot(newYaw);
		player.setXRot(Mth.clamp(newPitch, -90.0F, 90.0F));
	}

	private static float lerpAngle(float from, float to, float t) {
		float delta = Mth.wrapDegrees(to - from);
		return from + delta * t;
	}
}
