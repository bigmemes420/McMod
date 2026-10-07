package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Player (Meteor AutoTool / LB AutoTool): selects the hotbar tool with the best
 * destroy speed for the block under the crosshair while breaking.
 */
public final class AutoToolModule {
	private static boolean enabled;
	private static int previousSlot = -1;

	private AutoToolModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		previousSlot = -1;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.player.autotool.enabled"
						: "screen.rootymenu.menu.player.autotool.disabled"
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
		boolean breaking = client.options.keyAttack.isDown();
		if (!breaking) {
			if (previousSlot >= 0 && previousSlot < Inventory.getSelectionSize()) {
				player.getInventory().setSelectedSlot(previousSlot);
				previousSlot = -1;
			}
			return;
		}
		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			return;
		}
		BlockState state = client.level.getBlockState(blockHit.getBlockPos());
		Inventory inv = player.getInventory();
		int best = inv.getSelectedSlot();
		float bestSpeed = inv.getSelectedItem().getDestroySpeed(state);
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = inv.getItem(i);
			float speed = stack.getDestroySpeed(state);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				best = i;
			}
		}
		if (best != inv.getSelectedSlot()) {
			if (previousSlot < 0) {
				previousSlot = inv.getSelectedSlot();
			}
			inv.setSelectedSlot(best);
		}
	}
}
