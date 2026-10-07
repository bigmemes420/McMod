package com.example.client.module;

import com.example.client.config.ModConfig;
import com.example.client.mixin.KeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Visuals (Meteor Freecam / LB FreeCam): detaches the camera for WASD fly.
 * Outgoing position/look and block/entity interact packets are cancelled while
 * enabled — the body stays put on the server.
 */
public final class FreecamModule {
	public static final float MIN_SPEED = 0.1F;
	public static final float MAX_SPEED = 5.0F;
	public static final float DEFAULT_SPEED = 1.0F;

	private static boolean enabled;
	private static float speed = DEFAULT_SPEED;

	private static double x, y, z;
	private static double prevX, prevY, prevZ;
	private static float yaw, pitch;
	private static float prevYaw, prevPitch;
	private static float partialTick = 1.0F;
	private static CameraType savedPerspective;

	private FreecamModule() {
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

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (value) {
			activate(client);
		} else {
			deactivate(client);
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.freecam.enabled"
						: "screen.rootymenu.menu.visuals.freecam.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = false;
		if (value) {
			// Defer full activate until in-world tick
			enabled = true;
		}
	}

	private static void activate(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		Camera cam = client.gameRenderer.mainCamera();
		Vec3 pos = cam.position();
		x = prevX = pos.x;
		y = prevY = pos.y;
		z = prevZ = pos.z;
		yaw = prevYaw = player.getYRot();
		pitch = prevPitch = player.getXRot();
		savedPerspective = client.options.getCameraType();
		client.options.setCameraType(CameraType.FIRST_PERSON);
		unpressMovementKeys(client.options);
	}

	private static void deactivate(Minecraft client) {
		if (savedPerspective != null) {
			client.options.setCameraType(savedPerspective);
			savedPerspective = null;
		}
	}

	private static void unpressMovementKeys(Options options) {
		options.keyUp.setDown(false);
		options.keyDown.setDown(false);
		options.keyLeft.setDown(false);
		options.keyRight.setDown(false);
		options.keyJump.setDown(false);
		options.keyShift.setDown(false);
	}

	public static void setPartialTick(float pt) {
		partialTick = pt;
	}

	public static double getX() {
		return Mth.lerp(partialTick, prevX, x);
	}

	public static double getY() {
		return Mth.lerp(partialTick, prevY, y);
	}

	public static double getZ() {
		return Mth.lerp(partialTick, prevZ, z);
	}

	public static float getYaw() {
		return Mth.lerp(partialTick, prevYaw, yaw);
	}

	public static float getPitch() {
		return Mth.lerp(partialTick, prevPitch, pitch);
	}

	/** Mouse look while freecam is on (does not rotate the player body). */
	public static void changeLookDirection(double deltaYaw, double deltaPitch) {
		if (!enabled) {
			return;
		}
		prevYaw = yaw;
		prevPitch = pitch;
		yaw += (float) deltaYaw;
		pitch = Mth.clamp(pitch + (float) deltaPitch, -90.0F, 90.0F);
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}
		if (savedPerspective == null) {
			activate(client);
		}
		if (client.options.getCameraType() != CameraType.FIRST_PERSON) {
			client.options.setCameraType(CameraType.FIRST_PERSON);
		}

		Options options = client.options;
		boolean forward = isPhysicallyDown(options.keyUp);
		boolean backward = isPhysicallyDown(options.keyDown);
		boolean left = isPhysicallyDown(options.keyLeft);
		boolean right = isPhysicallyDown(options.keyRight);
		boolean up = isPhysicallyDown(options.keyJump);
		boolean down = isPhysicallyDown(options.keyShift);
		double s = isPhysicallyDown(options.keySprint) ? speed : speed * 0.5;

		Vec3 fwd = Vec3.directionFromRotation(0.0F, yaw);
		Vec3 rightVec = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
		double velX = 0.0;
		double velY = 0.0;
		double velZ = 0.0;
		boolean a = false;
		boolean b = false;
		if (forward) {
			velX += fwd.x * s;
			velZ += fwd.z * s;
			a = true;
		}
		if (backward) {
			velX -= fwd.x * s;
			velZ -= fwd.z * s;
			a = true;
		}
		if (right) {
			velX += rightVec.x * s;
			velZ += rightVec.z * s;
			b = true;
		}
		if (left) {
			velX -= rightVec.x * s;
			velZ -= rightVec.z * s;
			b = true;
		}
		if (a && b) {
			double diag = 1.0 / Math.sqrt(2.0);
			velX *= diag;
			velZ *= diag;
		}
		if (up) {
			velY += s;
		}
		if (down) {
			velY -= s;
		}

		prevX = x;
		prevY = y;
		prevZ = z;
		prevYaw = yaw;
		prevPitch = pitch;
		x += velX;
		y += velY;
		z += velZ;

		// Keep movement keys from affecting the body
		unpressMovementKeys(options);
	}

	private static boolean isPhysicallyDown(KeyMapping mapping) {
		InputConstants.Key key = ((KeyMappingAccessor) mapping).rooty$getKey();
		if (key.getType() != InputConstants.Type.KEYBOARD) {
			return false;
		}
		return InputConstants.isKeyDown(key.getValue());
	}

	/** Cancel outgoing movement/look and interact-related packets. */
	public static boolean shouldCancelPacket(Packet<?> packet) {
		if (!enabled) {
			return false;
		}
		return packet instanceof ServerboundMovePlayerPacket
				|| packet instanceof ServerboundPlayerInputPacket
				|| packet instanceof ServerboundMoveVehiclePacket
				|| packet instanceof ServerboundInteractPacket
				|| packet instanceof ServerboundUseItemPacket
				|| packet instanceof ServerboundUseItemOnPacket
				|| packet instanceof ServerboundPlayerActionPacket;
	}

	public static boolean shouldBlockInteract() {
		return enabled;
	}

	public static boolean shouldFreezeInput() {
		return enabled;
	}

	public static boolean shouldRedirectLook(Object entity) {
		if (!enabled) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		return entity != null && entity == client.player;
	}
}
