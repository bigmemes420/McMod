package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Combat module: inflates living-entity bounding boxes client-side for easier
 * targeting. Size slider is extra half-width in blocks (0–1).
 * Never affects the local player (movement / own hitbox stay vanilla).
 */
public final class HitboxesModule {
	public static final float MIN_SIZE = 0.0F;
	public static final float MAX_SIZE = 1.0F;
	public static final float DEFAULT_SIZE = 0.25F;

	private static boolean enabled;
	private static float size = DEFAULT_SIZE;

	private HitboxesModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getSize() {
		return size;
	}

	public static void setSize(float value) {
		float clamped = Mth.clamp(value, MIN_SIZE, MAX_SIZE);
		if (size == clamped) {
			return;
		}
		size = clamped;
		ModConfig.save();
	}

	public static void loadSize(float value) {
		size = Mth.clamp(value, MIN_SIZE, MAX_SIZE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.hitboxes.enabled"
						: "screen.rootymenu.menu.combat.hitboxes.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** Called from {@link com.example.client.mixin.EntityMixin}. */
	public static AABB modifyBoundingBox(Entity entity, AABB original) {
		if (!enabled || size <= 0.0F || entity == null || original == null) {
			return original;
		}
		// Local player must never be inflated (reference, type, or UUID).
		if (entity instanceof LocalPlayer) {
			return original;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			LocalPlayer self = client.player;
			if (entity == self || entity.getUUID().equals(self.getUUID())) {
				return original;
			}
		}
		if (!(entity instanceof LivingEntity)) {
			return original;
		}
		return original.inflate(size);
	}
}
