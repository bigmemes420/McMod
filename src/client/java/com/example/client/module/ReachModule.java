package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Combat module: client-side entity interaction reach bonus (blocks).
 * Bonus is withheld while the look-ray hits a living/player target that fails
 * {@link CombatTargetingModule} (independent extended raycast — no hitResult flicker).
 * Best-effort — dedicated servers may still enforce vanilla reach.
 */
public final class ReachModule {
	public static final float MIN_BONUS = 0.0F;
	public static final float MAX_BONUS = 3.0F;
	public static final float DEFAULT_BONUS = 1.0F;

	private static final Identifier REACH_ID = ExampleMod.id("rooty_reach");
	private static final double VANILLA_ENTITY_REACH = 3.0D;

	private static boolean enabled;
	private static float bonus = DEFAULT_BONUS;

	private ReachModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getBonus() {
		return bonus;
	}

	public static void setBonus(float value) {
		float clamped = Mth.clamp(value, MIN_BONUS, MAX_BONUS);
		if (bonus == clamped) {
			return;
		}
		bonus = clamped;
		apply(Minecraft.getInstance().player);
		ModConfig.save();
	}

	public static void loadBonus(float value) {
		bonus = Mth.clamp(value, MIN_BONUS, MAX_BONUS);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		apply(Minecraft.getInstance().player);
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.reach.enabled"
						: "screen.rootymenu.menu.combat.reach.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		apply(player);
	}

	private static void apply(LocalPlayer player) {
		if (player == null) {
			return;
		}
		AttributeInstance attr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
		if (attr == null) {
			return;
		}
		boolean allow = enabled && bonus > 0.0F;
		if (allow && looksAtDisallowedTarget(player, VANILLA_ENTITY_REACH + bonus)) {
			allow = false;
		}
		if (allow) {
			attr.addOrUpdateTransientModifier(new AttributeModifier(
					REACH_ID, bonus, AttributeModifier.Operation.ADD_VALUE
			));
		} else {
			attr.removeModifier(REACH_ID);
		}
	}

	/** True when an extended look-ray hits an entity that targeting rejects. */
	private static boolean looksAtDisallowedTarget(LocalPlayer player, double range) {
		Vec3 from = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 to = from.add(look.scale(range));
		AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				player,
				from,
				to,
				box,
				entity -> entity != player && entity.isPickable() && entity.isAlive(),
				range * range
		);
		if (hit == null) {
			return false;
		}
		Entity entity = hit.getEntity();
		return !CombatTargetingModule.matches(entity);
	}
}
