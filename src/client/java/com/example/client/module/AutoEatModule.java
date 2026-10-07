package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Player (Meteor AutoEat / LB SmartEat simplified): eats from the hotbar when
 * hunger drops below the threshold.
 */
public final class AutoEatModule {
	public static final float MIN_HUNGER = 1.0F;
	public static final float MAX_HUNGER = 19.0F;
	public static final float DEFAULT_HUNGER = 14.0F;

	private static boolean enabled;
	private static float hungerThreshold = DEFAULT_HUNGER;
	private static int previousSlot = -1;

	private AutoEatModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getHungerThreshold() {
		return hungerThreshold;
	}

	public static void setHungerThreshold(float value) {
		float clamped = Mth.clamp(value, MIN_HUNGER, MAX_HUNGER);
		if (hungerThreshold == clamped) {
			return;
		}
		hungerThreshold = clamped;
		ModConfig.save();
	}

	public static void loadHungerThreshold(float value) {
		hungerThreshold = Mth.clamp(value, MIN_HUNGER, MAX_HUNGER);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		previousSlot = -1;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.player.autoeat.enabled"
						: "screen.rootymenu.menu.player.autoeat.disabled"
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
		if (player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		if (player.isUsingItem()) {
			return;
		}
		if (player.getFoodData().getFoodLevel() > hungerThreshold) {
			if (previousSlot >= 0) {
				player.getInventory().setSelectedSlot(previousSlot);
				previousSlot = -1;
			}
			return;
		}
		Inventory inv = player.getInventory();
		int foodSlot = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = inv.getItem(i);
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null) {
				foodSlot = i;
				break;
			}
		}
		if (foodSlot < 0) {
			return;
		}
		if (previousSlot < 0) {
			previousSlot = inv.getSelectedSlot();
		}
		inv.setSelectedSlot(foodSlot);
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
	}
}
