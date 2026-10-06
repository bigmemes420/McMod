package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Combat module: inflates living-entity bounding boxes client-side for easier
 * targeting. Size slider is extra half-width in blocks (0–1).
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
				enabled ? "screen.modid.menu.combat.hitboxes.enabled"
						: "screen.modid.menu.combat.hitboxes.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** Called from {@link com.example.client.mixin.EntityMixin}. */
	public static AABB modifyBoundingBox(Entity entity, AABB original) {
		if (!enabled || size <= 0.0F) {
			return original;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || entity == client.player) {
			return original;
		}
		if (!(entity instanceof LivingEntity)) {
			return original;
		}
		return original.inflate(size);
	}
}
