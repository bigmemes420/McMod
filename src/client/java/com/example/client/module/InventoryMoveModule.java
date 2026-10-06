package com.example.client.module;

import com.example.client.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Player module: keep WASD/jump/sneak/sprint working while a container GUI is
 * open, and rotate with arrow keys (speed slider).
 */
public final class InventoryMoveModule {
	public static final float MIN_ROTATE_SPEED = 1.0F;
	public static final float MAX_ROTATE_SPEED = 40.0F;
	public static final float DEFAULT_ROTATE_SPEED = 12.0F;

	private static boolean enabled;
	private static float rotateSpeed = DEFAULT_ROTATE_SPEED;

	private InventoryMoveModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getRotateSpeed() {
		return rotateSpeed;
	}

	public static void setRotateSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_ROTATE_SPEED, MAX_ROTATE_SPEED);
		if (rotateSpeed == clamped) {
			return;
		}
		rotateSpeed = clamped;
		ModConfig.save();
	}

	public static void loadRotateSpeed(float value) {
		rotateSpeed = Mth.clamp(value, MIN_ROTATE_SPEED, MAX_ROTATE_SPEED);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.player.inventory_move.enabled"
						: "screen.modid.menu.player.inventory_move.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static boolean shouldPassMovement(Minecraft client) {
		return enabled && client != null && client.gui.screen() instanceof AbstractContainerScreen;
	}

	/** Arrow-key look while a container is open. */
	public static void tick(Minecraft client) {
		if (!shouldPassMovement(client)) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		double speed = rotateSpeed;
		double yaw = 0.0D;
		double pitch = 0.0D;
		if (InputConstants.isKeyDown(InputConstants.KEY_LEFT)) {
			yaw -= speed;
		}
		if (InputConstants.isKeyDown(InputConstants.KEY_RIGHT)) {
			yaw += speed;
		}
		if (InputConstants.isKeyDown(InputConstants.KEY_UP)) {
			pitch -= speed;
		}
		if (InputConstants.isKeyDown(InputConstants.KEY_DOWN)) {
			pitch += speed;
		}
		if (yaw != 0.0D || pitch != 0.0D) {
			player.turn(yaw, pitch);
		}
	}
}
