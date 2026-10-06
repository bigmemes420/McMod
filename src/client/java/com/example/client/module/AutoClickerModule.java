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
 * Combat module: client-side auto-attack while looking at an entity.
 * CPS slider controls click rate; respects attack cooldown when CPS is low.
 */
public final class AutoClickerModule {
	public static final float MIN_CPS = 1.0F;
	public static final float MAX_CPS = 20.0F;
	public static final float DEFAULT_CPS = 8.0F;

	private static boolean enabled;
	private static float cps = DEFAULT_CPS;
	private static long lastClickMs;

	private AutoClickerModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getCps() {
		return cps;
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
		long interval = Math.max(1L, Math.round(1000.0 / cps));
		if (now - lastClickMs < interval) {
			return;
		}
		// Prefer full cooldown when CPS is at or below vanilla-ish rates
		if (cps <= 10.0F && player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}

		lastClickMs = now;
		client.gameMode.attack(player, entityHit.getEntity());
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
	}
}
