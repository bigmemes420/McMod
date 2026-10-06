package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Combat module: moves a Totem of Undying from the inventory into the offhand
 * when the offhand is empty or not already holding a totem.
 */
public final class AutoTotemModule {
	private static boolean enabled;
	private static int cooldownTicks;

	private AutoTotemModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		cooldownTicks = 0;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.combat.autototem.enabled"
						: "screen.modid.menu.combat.autototem.disabled"
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
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		ItemStack off = player.getOffhandItem();
		if (off.is(Items.TOTEM_OF_UNDYING)) {
			return;
		}

		Inventory inv = player.getInventory();
		int invIndex = -1;
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(Items.TOTEM_OF_UNDYING)) {
				invIndex = i;
				break;
			}
		}
		if (invIndex < 0) {
			return;
		}

		// Map Inventory index → InventoryMenu slot (hotbar 0–8 → 36–44, main 9–35 → 9–35).
		int menuSlot;
		if (invIndex < Inventory.getSelectionSize()) {
			menuSlot = InventoryMenu.USE_ROW_SLOT_START + invIndex;
		} else {
			menuSlot = invIndex;
		}

		if (player.containerMenu != player.inventoryMenu) {
			return;
		}
		// SWAP with button = offhand inventory index moves clicked slot ↔ offhand.
		client.gameMode.handleContainerInput(
				player.inventoryMenu.containerId,
				menuSlot,
				Inventory.SLOT_OFFHAND,
				ContainerInput.SWAP,
				player
		);
		cooldownTicks = 5;
	}
}
