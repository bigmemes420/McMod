package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

/**
 * Combat module: when looking at an entity with full attack cooldown while
 * grounded, hop so the next hit can crit. Does not attack on its own.
 */
public final class CriticalsModule {
	private static boolean enabled;

	private CriticalsModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.criticals.enabled"
						: "screen.rootymenu.menu.combat.criticals.disabled"
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
		if (player == null || client.gui.screen() != null) {
			return;
		}
		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
			return;
		}
		if (player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}
		if (player.onGround() && !player.isInWater() && !player.onClimbable()) {
			player.jumpFromGround();
		}
	}
}
