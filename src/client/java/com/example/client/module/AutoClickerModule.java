package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Combat module: client-side auto-attack while looking at an entity.
 * CPS slider controls click rate; optional randomization adds 0–N ms of jitter
 * on top of the base interval.
 */
public final class AutoClickerModule {
	public static final float MIN_CPS = 1.0F;
	public static final float MAX_CPS = 20.0F;
	public static final float DEFAULT_CPS = 8.0F;

	public static final float MIN_RANDOMIZE = 0.0F;
	public static final float MAX_RANDOMIZE = 100.0F;
	public static final float DEFAULT_RANDOMIZE = 0.0F;

	private static boolean enabled;
	private static float cps = DEFAULT_CPS;
	/** Max extra delay in milliseconds randomly added to each click interval. */
	private static float randomizeMs = DEFAULT_RANDOMIZE;
	private static long lastClickMs;
	private static long nextIntervalMs = 125L;

	private AutoClickerModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getCps() {
		return cps;
	}

	public static float getRandomizeMs() {
		return randomizeMs;
	}

	public static void setCps(float value) {
		float clamped = Mth.clamp(value, MIN_CPS, MAX_CPS);
		if (cps == clamped) {
			return;
		}
		cps = clamped;
		ModConfig.save();
	}

	public static void loadCps(float value) {
		cps = Mth.clamp(value, MIN_CPS, MAX_CPS);
	}

	public static void setRandomizeMs(float value) {
		float clamped = Mth.clamp(value, MIN_RANDOMIZE, MAX_RANDOMIZE);
		if (randomizeMs == clamped) {
			return;
		}
		randomizeMs = clamped;
		ModConfig.save();
	}

	public static void loadRandomizeMs(float value) {
		randomizeMs = Mth.clamp(value, MIN_RANDOMIZE, MAX_RANDOMIZE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.combat.autoclicker.enabled"
						: "screen.modid.menu.combat.autoclicker.disabled"
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
		if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
			return;
		}
		if (!(hit instanceof EntityHitResult entityHit)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastClickMs < nextIntervalMs) {
			return;
		}
		if (cps <= 10.0F && player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}

		lastClickMs = now;
		long base = Math.max(1L, Math.round(1000.0 / cps));
		long jitter = 0L;
		if (randomizeMs > 0.0F) {
			jitter = ThreadLocalRandom.current().nextLong(0L, (long) randomizeMs + 1L);
		}
		nextIntervalMs = base + jitter;

		client.gameMode.attack(player, entityHit.getEntity());
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
	}
}
