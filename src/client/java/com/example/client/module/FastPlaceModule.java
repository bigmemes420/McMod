package com.example.client.module;

import com.example.client.config.ModConfig;
import com.example.client.mixin.MinecraftAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;

/**
 * World module: reduces the right-click delay so blocks place faster.
 * Speed 1–10 maps to delay ticks 4→0 (10 = no delay).
 */
public final class FastPlaceModule {
	public static final float MIN_SPEED = 1.0F;
	public static final float MAX_SPEED = 10.0F;
	public static final float DEFAULT_SPEED = 10.0F;

	private static boolean enabled;
	private static float speed = DEFAULT_SPEED;

	private FastPlaceModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getSpeed() {
		return speed;
	}

	public static void setSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
		if (speed == clamped) {
			return;
		}
		speed = clamped;
		ModConfig.save();
	}

	public static void loadSpeed(float value) {
		speed = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
	}

	/** Vanilla-ish delay: speed 10 → 0 ticks, speed 1 → 4 ticks. */
	public static int delayTicks() {
		float t = (MAX_SPEED - speed) / (MAX_SPEED - MIN_SPEED);
		return Mth.clamp(Math.round(t * 4.0F), 0, 4);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.world.fastplace.enabled"
						: "screen.rootymenu.menu.world.fastplace.disabled"
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
		if (!(player.getMainHandItem().getItem() instanceof BlockItem)
				&& !(player.getOffhandItem().getItem() instanceof BlockItem)) {
			return;
		}
		MinecraftAccessor accessor = (MinecraftAccessor) (Object) client;
		int delay = delayTicks();
		if (accessor.rooty$getRightClickDelay() > delay) {
			accessor.rooty$setRightClickDelay(delay);
		}
	}
}
