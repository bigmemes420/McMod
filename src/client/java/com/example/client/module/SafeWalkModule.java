package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Movement: sneak-edge behavior without holding sneak — stop before walking off
 * solid blocks when on ground.
 */
public final class SafeWalkModule {
	private static boolean enabled;

	private SafeWalkModule() {
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
				enabled ? "screen.rootymenu.menu.movement.safewalk.enabled"
						: "screen.rootymenu.menu.movement.safewalk.disabled"
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
		if (player == null || client.level == null || !player.onGround() || player.isShiftKeyDown()) {
			return;
		}
		Vec3 vel = player.getDeltaMovement();
		if (Math.abs(vel.x) < 1.0E-4D && Math.abs(vel.z) < 1.0E-4D) {
			return;
		}
		AABB box = player.getBoundingBox();
		double ox = vel.x;
		double oz = vel.z;
		double nx = ox;
		double nz = oz;

		// Probe one step ahead; if that footing is air, zero the axis that would fall.
		if (!hasGroundSupport(client, box.move(ox, -0.05D, 0.0D))) {
			nx = 0.0D;
		}
		if (!hasGroundSupport(client, box.move(0.0D, -0.05D, oz))) {
			nz = 0.0D;
		}
		if (nx != ox || nz != oz) {
			player.setDeltaMovement(nx, vel.y, nz);
		}
	}

	private static boolean hasGroundSupport(Minecraft client, AABB box) {
		int minX = (int) Math.floor(box.minX);
		int maxX = (int) Math.floor(box.maxX);
		int minZ = (int) Math.floor(box.minZ);
		int maxZ = (int) Math.floor(box.maxZ);
		int y = (int) Math.floor(box.minY - 0.05D);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				cursor.set(x, y, z);
				if (!client.level.getBlockState(cursor).getCollisionShape(client.level, cursor).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
}
