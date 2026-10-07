package com.example.client.module;

import com.example.client.config.ModConfig;
import com.example.client.mixin.FishingHookAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/**
 * Automations (Meteor AutoFish / LB AutoFish simplified): cast and reel with
 * vanilla useItem when the bobber bites. No packet spoofing.
 */
public final class AutoFishModule {
	public static final float MIN_CATCH_DELAY = 1.0F;
	public static final float MAX_CATCH_DELAY = 20.0F;
	public static final float DEFAULT_CATCH_DELAY = 6.0F;
	public static final float MIN_RECAST_DELAY = 5.0F;
	public static final float MAX_RECAST_DELAY = 40.0F;
	public static final float DEFAULT_RECAST_DELAY = 14.0F;

	private static boolean enabled;
	private static float catchDelayTicks = DEFAULT_CATCH_DELAY;
	private static float recastDelayTicks = DEFAULT_RECAST_DELAY;
	private static int cooldown;
	private static boolean waitingToReel;

	private AutoFishModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getCatchDelay() {
		return catchDelayTicks;
	}

	public static void setCatchDelay(float value) {
		float clamped = Mth.clamp(value, MIN_CATCH_DELAY, MAX_CATCH_DELAY);
		if (catchDelayTicks == clamped) {
			return;
		}
		catchDelayTicks = clamped;
		ModConfig.save();
	}

	public static void loadCatchDelay(float value) {
		catchDelayTicks = Mth.clamp(value, MIN_CATCH_DELAY, MAX_CATCH_DELAY);
	}

	public static float getRecastDelay() {
		return recastDelayTicks;
	}

	public static void setRecastDelay(float value) {
		float clamped = Mth.clamp(value, MIN_RECAST_DELAY, MAX_RECAST_DELAY);
		if (recastDelayTicks == clamped) {
			return;
		}
		recastDelayTicks = clamped;
		ModConfig.save();
	}

	public static void loadRecastDelay(float value) {
		recastDelayTicks = Mth.clamp(value, MIN_RECAST_DELAY, MAX_RECAST_DELAY);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		cooldown = 0;
		waitingToReel = false;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.automations.autofish.enabled"
						: "screen.rootymenu.menu.automations.autofish.disabled"
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
		if (cooldown > 0) {
			cooldown--;
			return;
		}

		FishingHook hook = player.fishing;
		if (hook != null && !hook.isRemoved()) {
			if (hook.getHookedIn() != null) {
				useRod(client, player);
				waitingToReel = false;
				cooldown = Math.round(recastDelayTicks);
				return;
			}
			boolean biting = ((FishingHookAccessor) hook).rooty$isBiting();
			if (biting) {
				if (!waitingToReel) {
					waitingToReel = true;
					cooldown = Math.round(catchDelayTicks);
					return;
				}
				useRod(client, player);
				waitingToReel = false;
				cooldown = Math.round(recastDelayTicks);
			} else {
				waitingToReel = false;
			}
			return;
		}

		waitingToReel = false;
		InteractionHand hand = findRodHand(player);
		if (hand == null) {
			return;
		}
		client.gameMode.useItem(player, hand);
		cooldown = Math.round(recastDelayTicks);
	}

	private static void useRod(Minecraft client, LocalPlayer player) {
		InteractionHand hand = findRodHand(player);
		if (hand == null) {
			return;
		}
		client.gameMode.useItem(player, hand);
	}

	private static InteractionHand findRodHand(LocalPlayer player) {
		if (player.getMainHandItem().getItem() instanceof FishingRodItem) {
			return InteractionHand.MAIN_HAND;
		}
		if (player.getOffhandItem().getItem() instanceof FishingRodItem) {
			return InteractionHand.OFF_HAND;
		}
		Inventory inv = player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() instanceof FishingRodItem) {
				inv.setSelectedSlot(i);
				return InteractionHand.MAIN_HAND;
			}
		}
		return null;
	}
}
