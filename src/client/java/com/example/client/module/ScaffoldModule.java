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
	public enum Mode {
		HAND_ONLY,
		OFFHAND_ONLY,
		FROM_INVENTORY
	}

	private static boolean enabled;
	private static Mode mode = Mode.FROM_INVENTORY;

	private ScaffoldModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static void setMode(Mode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			mode = Mode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			mode = Mode.FROM_INVENTORY;
		}
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

		PlaceSource source = resolveSource(player);
		if (source == null) {
			return;
		}

		int prev = player.getInventory().getSelectedSlot();
		if (source.hand == InteractionHand.MAIN_HAND && source.hotbarSlot >= 0) {
			player.getInventory().setSelectedSlot(source.hotbarSlot);
		}

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
		client.gameMode.useItemOn(player, source.hand, hitResult);
		player.swing(source.hand, SwingAnimation.DEFAULT, false);
		player.getInventory().setSelectedSlot(prev);
	}

	private static PlaceSource resolveSource(LocalPlayer player) {
		return switch (mode) {
			case HAND_ONLY -> {
				ItemStack stack = player.getMainHandItem();
				if (stack.getItem() instanceof BlockItem) {
					yield new PlaceSource(InteractionHand.MAIN_HAND, player.getInventory().getSelectedSlot());
				}
				yield null;
			}
			case OFFHAND_ONLY -> {
				ItemStack stack = player.getOffhandItem();
				if (stack.getItem() instanceof BlockItem) {
					yield new PlaceSource(InteractionHand.OFF_HAND, -1);
				}
				yield null;
			}
			case FROM_INVENTORY -> {
				int slot = findHotbarBlock(player.getInventory());
				if (slot < 0) {
					yield null;
				}
				yield new PlaceSource(InteractionHand.MAIN_HAND, slot);
			}
		};
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

	private record PlaceSource(InteractionHand hand, int hotbarSlot) {
	}
}
