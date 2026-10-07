package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Combat (Meteor/LB AutoArmor simplified): equips better armor from inventory
 * into empty / weaker armor slots via container swaps.
 */
public final class AutoArmorModule {
	private static boolean enabled;
	private static int cooldown;

	private AutoArmorModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		cooldown = 0;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.combat.autoarmor.enabled"
						: "screen.rootymenu.menu.combat.autoarmor.disabled"
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
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		Inventory inv = player.getInventory();
		EquipmentSlot[] slots = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
		};
		for (EquipmentSlot slot : slots) {
			ItemStack equipped = player.getItemBySlot(slot);
			int bestInv = -1;
			float bestScore = score(equipped);
			for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
				ItemStack stack = inv.getItem(i);
				Equippable eq = stack.get(DataComponents.EQUIPPABLE);
				if (eq == null || eq.slot() != slot) {
					continue;
				}
				float s = score(stack);
				if (s > bestScore) {
					bestScore = s;
					bestInv = i;
				}
			}
			if (bestInv < 0) {
				continue;
			}
			int armorMenuSlot = switch (slot) {
				case HEAD -> 5;
				case CHEST -> 6;
				case LEGS -> 7;
				case FEET -> 8;
				default -> -1;
			};
			if (armorMenuSlot < 0) {
				continue;
			}
			int fromMenu = bestInv < Inventory.getSelectionSize()
					? InventoryMenu.USE_ROW_SLOT_START + bestInv
					: bestInv;
			// Quick-move into armor slot when empty; otherwise pickup-swap dance via QUICK_MOVE first try.
			client.gameMode.handleContainerInput(
					player.inventoryMenu.containerId,
					fromMenu,
					0,
					ContainerInput.QUICK_MOVE,
					player
			);
			cooldown = 8;
			return;
		}
	}

	private static float score(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return -1.0F;
		}
		float durability = stack.getMaxDamage() > 0
				? (float) (stack.getMaxDamage() - stack.getDamageValue())
				: 50.0F;
		int enchants = stack.getEnchantments().size();
		return durability + enchants * 25.0F;
	}
}
