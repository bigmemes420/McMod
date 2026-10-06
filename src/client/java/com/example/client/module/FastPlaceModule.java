package com.example.client.module;

import com.example.client.config.ModConfig;
import com.example.client.mixin.MinecraftAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;

/**
 * World module: clears the right-click delay so blocks place as fast as the game allows.
 */
public final class FastPlaceModule {
	private static boolean enabled;

	private FastPlaceModule() {
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
				enabled ? "screen.modid.menu.world.fastplace.enabled"
						: "screen.modid.menu.world.fastplace.disabled"
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
		if (player == null || client.gui.screen() != null) {
			return;
		}
		if (!(player.getMainHandItem().getItem() instanceof BlockItem)
				&& !(player.getOffhandItem().getItem() instanceof BlockItem)) {
			return;
		}
		((MinecraftAccessor) (Object) client).rooty$setRightClickDelay(0);
	}
}
