package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * World module: place blocks in mid-air when looking at empty space.
 * Distance slider (1–4) sets how far along the look vector the target block is.
 */
public final class AirPlaceModule {
	public static final float MIN_DISTANCE = 1.0F;
	public static final float MAX_DISTANCE = 4.0F;
	public static final float DEFAULT_DISTANCE = 4.0F;

	private static boolean enabled;
	private static float distance = DEFAULT_DISTANCE;

	private AirPlaceModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getDistance() {
		return distance;
	}

	public static void setDistance(float value) {
		float clamped = Mth.clamp(value, MIN_DISTANCE, MAX_DISTANCE);
		if (distance == clamped) {
			return;
		}
		distance = clamped;
		ModConfig.save();
	}

	public static void loadDistance(float value) {
		distance = Mth.clamp(value, MIN_DISTANCE, MAX_DISTANCE);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.world.airplace.enabled"
						: "screen.modid.menu.world.airplace.disabled"
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
		if (player == null || client.gameMode == null || client.level == null || client.gui.screen() != null) {
			return;
		}
		if (!client.options.keyUse.isDown()) {
			return;
		}

		InteractionHand hand = resolveHand(player);
		if (hand == null) {
			return;
		}

		HitResult hit = client.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
			return;
		}
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) hit;
			BlockState state = client.level.getBlockState(blockHit.getBlockPos());
			if (!state.canBeReplaced()) {
				return; // normal placement against a solid block
			}
		}

		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		double dist = distance;
		Vec3 target = eye.add(look.scale(dist));
		BlockPos placePos = BlockPos.containing(target);
		BlockState at = client.level.getBlockState(placePos);
		if (!at.canBeReplaced()) {
			return;
		}

		Direction face = Direction.getApproximateNearest(-look.x, -look.y, -look.z);
		Vec3 hitVec = Vec3.atCenterOf(placePos).subtract(look.scale(0.5D));
		BlockHitResult airHit = new BlockHitResult(hitVec, face, placePos, false);
		client.gameMode.useItemOn(player, hand, airHit);
		player.swing(hand, SwingAnimation.DEFAULT, false);
	}

	private static InteractionHand resolveHand(LocalPlayer player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof BlockItem) {
			return InteractionHand.MAIN_HAND;
		}
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof BlockItem) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}
}
