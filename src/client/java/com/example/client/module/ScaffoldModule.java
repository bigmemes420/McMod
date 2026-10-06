package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * World module: places blocks under the player's feet while moving (classic scaffold).
 */
public final class ScaffoldModule {
	private static boolean enabled;

	private ScaffoldModule() {
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
				enabled ? "screen.modid.menu.world.scaffold.enabled"
						: "screen.modid.menu.world.scaffold.disabled"
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
		if (player.getAbilities().flying) {
			return;
		}

		BlockPos feet = player.blockPosition();
		BlockPos below = feet.below();
		BlockState belowState = client.level.getBlockState(below);
		if (!belowState.canBeReplaced()) {
			return;
		}

		int slot = findHotbarBlock(player.getInventory());
		if (slot < 0) {
			return;
		}

		int prev = player.getInventory().getSelectedSlot();
		player.getInventory().setSelectedSlot(slot);

		// Prefer placing against a neighboring solid face under the player.
		Direction placeFace = Direction.UP;
		BlockPos neighbor = below.below();
		BlockState neighborState = client.level.getBlockState(neighbor);
		if (neighborState.canBeReplaced()) {
			boolean found = false;
			for (Direction dir : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN }) {
				BlockPos n = below.relative(dir);
				if (!client.level.getBlockState(n).canBeReplaced()) {
					neighbor = n;
					placeFace = dir.getOpposite();
					found = true;
					break;
				}
			}
			if (!found) {
				player.getInventory().setSelectedSlot(prev);
				return;
			}
		}

		Vec3 hit = Vec3.atCenterOf(neighbor).add(
				placeFace.getStepX() * 0.5D,
				placeFace.getStepY() * 0.5D,
				placeFace.getStepZ() * 0.5D
		);
		BlockHitResult hitResult = new BlockHitResult(hit, placeFace, neighbor, false);
		client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
		player.getInventory().setSelectedSlot(prev);
	}

	static int findHotbarBlock(Inventory inv) {
		int selected = inv.getSelectedSlot();
		ItemStack held = inv.getItem(selected);
		if (held.getItem() instanceof BlockItem) {
			return selected;
		}
		int size = Inventory.getSelectionSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() instanceof BlockItem) {
				return i;
			}
		}
		return -1;
	}
}
