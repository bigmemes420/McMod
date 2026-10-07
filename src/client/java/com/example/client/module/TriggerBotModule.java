package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Combat module: attacks once when the crosshair is on an entity and the
 * attack cooldown is ready (no CPS spam — unlike AutoClicker).
 */
public final class TriggerBotModule {
	public static final float MIN_DELAY = 0.0F;
	public static final float MAX_DELAY = 10.0F;
	public static final float DEFAULT_DELAY = 0.0F;

	private static boolean enabled;
	private static float delayTicks = DEFAULT_DELAY;
	private static long lastAttackMs;

	private TriggerBotModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getDelay() {
		return delayTicks;
	}

	public static void setDelay(float value) {
		float clamped = Mth.clamp(value, MIN_DELAY, MAX_DELAY);
		if (delayTicks == clamped) {
			return;
		}
		delayTicks = clamped;
		ModConfig.save();
	}

	public static void loadDelay(float value) {
		delayTicks = Mth.clamp(value, MIN_DELAY, MAX_DELAY);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.triggerbot.enabled"
						: "screen.rootymenu.menu.combat.triggerbot.disabled"
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
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.ENTITY || !(hit instanceof EntityHitResult entityHit)) {
			return;
		}
		if (player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}
		long now = System.currentTimeMillis();
		long wait = Math.round(delayTicks * 50.0);
		if (now - lastAttackMs < wait) {
			return;
		}
		lastAttackMs = now;
		client.gameMode.attack(player, entityHit.getEntity());
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
	}
}
