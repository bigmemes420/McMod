package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Movement (Meteor Parkour): jumps at block edges when standing on ground.
 */
public final class ParkourModule {
	private static boolean enabled;

	private ParkourModule() {
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
				enabled ? "screen.rootymenu.menu.movement.parkour.enabled"
						: "screen.rootymenu.menu.movement.parkour.disabled"
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
		if (player == null || client.level == null || client.gui.screen() != null) {
			return;
		}
		if (!player.onGround() || client.options.keyJump.isDown() || player.isShiftKeyDown()) {
			return;
		}
		AABB box = player.getBoundingBox();
		AABB adjusted = box.move(0.0D, -0.5D, 0.0D).inflate(-0.01D, 0.0D, -0.01D);
		if (client.level.getBlockCollisions(player, adjusted).iterator().hasNext()) {
			return;
		}
		player.jumpFromGround();
	}
}
