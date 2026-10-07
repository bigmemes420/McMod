package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Misc module: when ON, module toggles send chat system messages.
 * Defaults to ON so existing modules keep notifying until the user turns it off.
 */
public final class NotificationsModule {
	private static boolean enabled = true;

	private NotificationsModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		// Only announce when turning ON (when OFF, notifications are suppressed).
		notifyToggle(
				enabled ? "screen.rootymenu.menu.misc.notifications.enabled"
						: "screen.rootymenu.menu.misc.notifications.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Send a module chat notification if Notifications is ON and a local player exists.
	 */
	public static void notifyToggle(String translationKey) {
		notifyMessage(Component.translatable(translationKey));
	}

	public static void notifyMessage(Component message) {
		if (!enabled || message == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player != null) {
			player.sendSystemMessage(message);
		}
	}
}
