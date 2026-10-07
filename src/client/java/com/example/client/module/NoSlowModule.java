package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Player module: cancel item-use movement slowdown and block stuck-speed
 * (e.g. cobwebs).
 */
public final class NoSlowModule {
	private static boolean enabled;

	private NoSlowModule() {
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
				enabled ? "screen.rootymenu.menu.player.noslow.enabled"
						: "screen.rootymenu.menu.player.noslow.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** Skip item-use input scale when enabled. */
	public static boolean shouldCancelItemSlowdown() {
		return enabled;
	}

	/**
	 * Called from {@code Entity.makeStuckInBlock}: when enabled, do not apply
	 * stuck speed multipliers (cobweb / berry bush / etc.).
	 */
	public static boolean shouldCancelStuckInBlock(Entity entity, BlockState state, Vec3 multiplier) {
		return enabled;
	}
}
