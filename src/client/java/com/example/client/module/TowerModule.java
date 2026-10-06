package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * World module: while jumping with a block in hand, rapidly place under feet (tower up).
 */
public final class TowerModule {
	private static boolean enabled;

	private TowerModule() {
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
				enabled ? "screen.modid.menu.world.tower.enabled"
						: "screen.modid.menu.world.tower.disabled"
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
		if (!client.options.keyJump.isDown()) {
			return;
		}

		int slot = ScaffoldModule.findHotbarBlock(player.getInventory());
		if (slot < 0) {
			return;
		}

		BlockPos below = player.blockPosition().below();
		BlockState belowState = client.level.getBlockState(below);
		if (!belowState.canBeReplaced()) {
			// Already standing on something — nudge upward while holding jump.
			if (player.onGround()) {
				player.setDeltaMovement(player.getDeltaMovement().x, 0.42D, player.getDeltaMovement().z);
			}
			return;
		}

		int prev = player.getInventory().getSelectedSlot();
		player.getInventory().setSelectedSlot(slot);

		BlockPos neighbor = below.below();
		Direction face = Direction.UP;
		if (client.level.getBlockState(neighbor).canBeReplaced()) {
			player.getInventory().setSelectedSlot(prev);
			return;
		}

		Vec3 hit = Vec3.atCenterOf(neighbor).add(0.0D, 0.5D, 0.0D);
		BlockHitResult hitResult = new BlockHitResult(hit, face, neighbor, false);
		client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
		player.setDeltaMovement(player.getDeltaMovement().x, 0.42D, player.getDeltaMovement().z);
		player.getInventory().setSelectedSlot(prev);
	}
}
