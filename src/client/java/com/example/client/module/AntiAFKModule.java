package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Player (Meteor AntiAFK simplified): small yaw spins and occasional hand swings
 * so the client looks active. No packet spoofing / anti-cheat evasion.
 */
public final class AntiAFKModule {
	public static final float MIN_INTERVAL = 20.0F;
	public static final float MAX_INTERVAL = 200.0F;
	public static final float DEFAULT_INTERVAL = 40.0F;

	private static boolean enabled;
	private static float intervalTicks = DEFAULT_INTERVAL;
	private static int cooldown;

	private AntiAFKModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getInterval() {
		return intervalTicks;
	}

	public static void setInterval(float value) {
		float clamped = Mth.clamp(value, MIN_INTERVAL, MAX_INTERVAL);
		if (intervalTicks == clamped) {
			return;
		}
		intervalTicks = clamped;
		ModConfig.save();
	}

	public static void loadInterval(float value) {
		intervalTicks = Mth.clamp(value, MIN_INTERVAL, MAX_INTERVAL);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		cooldown = 0;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.player.antiafk.enabled"
						: "screen.rootymenu.menu.player.antiafk.disabled"
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
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		cooldown = Math.round(intervalTicks);
		player.setYRot(Mth.wrapDegrees(player.getYRot() + 15.0F));
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
	}
}
